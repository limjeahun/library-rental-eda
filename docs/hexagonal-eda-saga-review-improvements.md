# Hexagonal EDA-SAGA Review Improvements

## 목적

이 문서는 현재 `library-rental-eda` 코드의 Hexagonal Architecture, DDD, EDA-SAGA 리뷰 결과를 바탕으로 개선 방향을 정리한다.

우선순위는 실제 장애 상황에서 데이터 정합성이 깨질 가능성이 큰 항목을 먼저 둔다. 이 프로젝트의 `AGENTS.md` 예외 규칙에 따라 Outbox, DLQ/DLT, 커스텀 Kafka retry/backoff, 분산 추적, 직접 서비스 간 HTTP 호출은 개선 범위에 포함하지 않는다.

## 개선 원칙

- 서비스 간 상태 변경은 Kafka command/event/result 흐름으로만 처리한다.
- `eventId`는 메시지 자체의 고유 식별자로 사용한다.
- `correlationId`는 하나의 비즈니스 흐름을 묶는 식별자로 유지한다.
- Result Event와 Domain Event는 목적과 이름을 분리한다.
- Consumer 멱등성과 보상 트랜잭션 멱등성을 별도로 설계한다.
- Consumer는 역직렬화, 최소 검증, 멱등 체크, use case 위임까지만 담당한다.
- 보상 결정과 상태 변경은 application/domain에 둔다.
- application 계층은 adapter/config 패키지에 직접 의존하지 않게 한다.

## P0. Consumer 멱등성 리팩터링

### 현재 흐름

현재 Kafka Consumer의 멱등 처리는 Redis에 `processed:{service}:{eventId}` 키를 두는 방식이다.

일반적인 처리 흐름:

1. `redisTemplate.hasKey(key)`로 처리 여부 확인
2. 처리되지 않았으면 비즈니스 로직 실행
3. 성공 후 `redisTemplate.opsForValue().set(key, "1", TTL)`로 처리 완료 기록

### 문제

이 흐름에는 두 가지 핵심 문제가 있다.

- 동시 도착 race condition: 같은 메시지가 동시에 들어오면 둘 다 `hasKey=false`를 보고 둘 다 비즈니스 로직을 실행할 수 있다.
- DB 상태와 멱등성 키 불일치: 비즈니스 로직 또는 DB 트랜잭션은 실패했는데 Redis 키가 남거나, 반대로 DB는 커밋됐는데 Redis 키 저장이 실패하면 재처리 시 중복 상태 변경이 발생할 수 있다.

### 개선안

멱등 처리를 "원자적 점유"와 "처리 완료 기록"으로 분리한다.

1차 방어는 Redis `SETNX` 또는 `SET key value NX EX`로 원자적으로 처리 권한을 점유한다.

```java
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(processingKey, ownerValue, processingTtl);

if (!Boolean.TRUE.equals(acquired)) {
    log.info("skip already claimed message eventId={}", eventId);
    return;
}
```

2차 방어는 Idempotent Receiver 패턴으로 DB 트랜잭션과 처리 완료 기록을 일관되게 묶는다.

권장 방향:

- Redis `processing:{service}:{eventId}` 키는 동시 소비 방지를 위한 짧은 점유 락으로 사용한다.
- 최종 처리 완료 여부는 서비스 소유 저장소의 processed message store에 기록한다.
- 비즈니스 상태 변경과 processed message record 저장을 같은 DB 트랜잭션 안에서 처리한다.
- processed message store에는 `eventId` 또는 `service + eventId`에 unique 제약을 둔다.
- unique 제약 위반은 이미 처리된 메시지로 보고 비즈니스 로직을 실행하지 않는다.

예상 저장소 개념:

```text
processed_message
├── service_name
├── event_id
├── correlation_id
├── message_type
├── processed_at
└── unique(service_name, event_id)
```

서비스별 적용 방식:

- book/member/rental 서비스: MariaDB `processed_message` 테이블과 unique constraint 사용
- bestbook 서비스: MongoDB `processed_messages` collection과 `serviceName + eventId` unique index 사용

Redis만으로 최종 처리 완료 키까지 관리하면 DB 트랜잭션과 원자적으로 커밋할 수 없다. 따라서 Redis는 동시 실행 방지용 claim으로 제한하고, 정합성의 최종 기준은 DB 트랜잭션 안의 processed message record로 두는 편이 안전하다.

### 분기 규칙

Consumer 또는 멱등성 포트는 점유 실패와 첫 처리를 명확히 구분한다.

- `CLAIMED`: 현재 인스턴스가 처음 처리할 권한을 얻었다. use case를 호출한다.
- `ALREADY_PROCESSING`: 다른 consumer/thread가 처리 중이다. 현재 메시지는 스킵하거나 재전달을 기다린다.
- `ALREADY_PROCESSED`: DB의 processed message record 또는 Redis processed key 기준으로 이미 완료된 메시지다. 비즈니스 로직을 실행하지 않는다.
- `FAILED_AND_RELEASED`: 처리 중 예외가 발생했고 processing key를 해제했다. Kafka 재전달 시 다시 처리될 수 있다.

### 헥사고널 배치

두 가지 배치 중 하나로 통일한다.

#### Adapter-local 방식

작은 범위의 기술 멱등성은 `adapter/in/messaging/consumer`에 둔다.

- Consumer가 Redis `SETNX`로 processing key를 점유한다.
- 점유 성공 시 application use case에 위임한다.
- 성공 시 processed 기록을 남긴다.
- 실패 시 processing key를 삭제하고 예외를 전파하거나 실패 Result Event를 발행한다.

이 방식은 구현이 단순하지만 DB 커밋과 Redis processed 저장을 완전히 원자화할 수 없다.

#### IdempotencyPort 방식

정합성이 중요한 Consumer는 별도 port를 둔다.

예상 구조:

```text
application/
  port/
    out/
      MessageIdempotencyPort.java
adapter/
  out/
    persistence/
      ProcessedMessageJpaEntity.java
      ProcessedMessageJpaRepository.java
      ProcessedMessageMongoDocument.java
      ProcessedMessageMongoRepository.java
      MessageIdempotencyPersistenceAdapter.java
  in/
    messaging/
      consumer/
```

각 서비스는 자기 저장 기술에 맞는 구현만 둔다. MariaDB 기반 서비스는 JPA entity/repository를 사용하고, bestbook-service는 MongoDB document/repository를 사용한다.

예상 port:

```java
public interface MessageIdempotencyPort {
    boolean isProcessed(String serviceName, String eventId);
    boolean markProcessed(String serviceName, String eventId, String correlationId, String messageType);
}
```

적용 방향:

- Consumer의 Redis `SETNX`는 동시 처리 방지용으로 유지할 수 있다.
- application service는 비즈니스 상태 변경과 `markProcessed(...)`를 같은 `@Transactional` 경계에서 실행한다.
- `markProcessed(...)`가 unique 제약 위반으로 실패하면 이미 처리된 메시지로 보고 도메인 상태 변경을 건너뛴다.
- 멱등성 판단은 비즈니스 규칙이 아니므로 domain에는 두지 않는다.

## P0. Result Event 식별자 분리

### 문제

현재 `EventResult.eventId`가 원본 이벤트의 `eventId`를 재사용한다.

대표 위치:

- `book-service/src/main/java/com/example/library/book/application/service/BookRentalEventService.java`
- `member-service/src/main/java/com/example/library/member/application/service/MemberEventService.java`
- `rental-service/src/main/java/com/example/library/rental/adapter/in/messaging/consumer/RentalEventConsumer.java`

이 구조에서는 같은 원본 이벤트에 대해 book-service와 member-service가 각각 Result Event를 발행해도 rental-service가 두 번째 결과를 중복 메시지로 오인할 수 있다.

### 개선안

`EventResult.eventId`는 Result Event 자체의 새 UUID로 생성한다. 원본 이벤트 식별자는 별도 필드로 보존한다.

최종 `EventResult` 계약은 아래 `P0. SAGA Result 누락 및 참여자 결과 추적 보강` 섹션의 `participant`, `step` 포함 계약을 기준으로 한다.

적용 방향:

- book/member 서비스는 Result Event 생성 시 `eventId = UUID.randomUUID().toString()`을 사용한다.
- 원본 `ItemRented`, `ItemReturned`, `OverdueCleared`의 `eventId`는 `sourceEventId`에 넣는다.
- Result Event에는 참여자와 처리 단계를 구분할 수 있는 `participant`, `step`을 반드시 포함한다.
- rental-service의 메시지 멱등 체크는 계속 `result.eventId()` 기준으로 수행한다.
- 추적 로그에는 `eventId`, `sourceEventId`, `correlationId`, `participant`, `step`을 함께 남긴다.

## P0. SAGA Result 누락 및 참여자 결과 추적 보강

### 문제

현재 SAGA 참여 서비스의 처리 결과가 일관되게 발행되지 않는다. 특히 `MemberEventService.handleRent()`와 `handleReturn()`은 포인트 적립 실패 시 로그만 남기고 Result Event를 발행하지 않는다.

대표 위치:

- `member-service/src/main/java/com/example/library/member/application/service/MemberEventService.java`
- `rental-service/src/main/java/com/example/library/rental/application/service/RentalResultService.java`

Consumer가 먼저 Redis에 `eventId`를 처리 완료로 기록한 뒤 use case를 호출하기 때문에, 처리 중 예외가 발생하면 재처리도 보상도 일어나지 않는다.

또한 rental-service의 `RentalResultService`는 `EventType.RENT`만 보고 보상 분기를 한다. 이 상태에서는 RENT 흐름에서 book-service 실패인지 member-service 포인트 실패인지 구분할 수 없다. 결과적으로 이미 성공한 참여자만 보상해야 하는데, 어떤 참여자가 성공했는지 판단하기 어렵다.

### 개선안

Choreography 방식을 유지하되, rental-service가 자신이 시작한 흐름의 참여 결과를 `correlationId` 기준으로 추적할 수 있게 보강한다.

핵심 방향:

- 모든 참여 서비스는 성공/실패 모두 `EventResult`를 발행한다.
- `EventResult`에는 어떤 참여자의 어떤 단계 결과인지 식별할 수 있는 필드를 추가한다.
- rental-service는 단일 실패 Result만 보고 즉시 동일 보상을 실행하지 않고, `correlationId` 단위로 참여 결과 상태를 해석한다.
- timeout은 자동 보상보다 먼저 로컬 상태 추적과 운영 감지 대상으로 도입한다.

### EventResult 계약 보강

`EventType.RENT`, `EventType.RETURN`, `EventType.OVERDUE`만으로는 참여자와 단계를 구분하기 어렵다. Result Event에 참여자와 처리 단계를 추가한다.

이 섹션의 계약을 문서 전체의 최종 `EventResult` 기준으로 삼는다.

후보 enum:

```java
public enum Participant {
    BOOK,
    MEMBER
}

public enum SagaStep {
    BOOK_MAKE_UNAVAILABLE,
    BOOK_MAKE_AVAILABLE,
    MEMBER_SAVE_POINT,
    MEMBER_USE_POINT
}
```

`Participant`와 `SagaStep`은 서비스 간 결과 메시지 해석에 필요한 공유 프로토콜 enum이므로 `common-events`에 둔다.

예상 계약:

```java
public record EventResult(
    String eventId,
    String correlationId,
    String sourceEventId,
    Instant occurredAt,
    EventType eventType,
    Participant participant,
    SagaStep step,
    boolean successed,
    IDName idName,
    Item item,
    long point,
    String reason
) {
}
```

적용 방향:

- book-service는 `Participant.BOOK`과 도서 상태 변경 step을 포함해 Result Event를 발행한다.
- member-service는 `Participant.MEMBER`와 포인트 적립/차감 step을 포함해 Result Event를 발행한다.
- rental-service는 `eventType + participant + step + successed` 조합으로 보상 대상을 판단한다.

### member-service 발행 규칙

member-service의 모든 참여 흐름은 성공/실패 Result Event를 발행한다.

적용 방향:

- `handleRent(ItemRented event)` 성공 시 `EventType.RENT` 성공 Result Event 발행
- `handleRent(ItemRented event)` 실패 시 `EventType.RENT` 실패 Result Event 발행
- `handleReturn(ItemReturned event)` 성공 시 `EventType.RETURN` 성공 Result Event 발행
- `handleReturn(ItemReturned event)` 실패 시 `EventType.RETURN` 실패 Result Event 발행
- `handleOverdueClear(OverdueCleared event)` 성공/실패 Result Event 발행 유지
- `handlePointUse(PointUseCommand command)`도 보상 command 처리 결과를 발행할지 검토한다.
- 기존 `handleOverdueClear()`와 같은 try/catch/publish 골격으로 통일

`PointUseCommand`의 결과는 정상 RENT/RETURN 흐름의 결과와 의미가 다르다. 따라서 동일한 `EventType.RENT`에 섞기보다 `SagaStep.MEMBER_USE_POINT` 또는 별도 보상 result 타입으로 의미를 구분하는 편이 안전하다.

### rental-service RENT 처리 규칙

RENT 흐름에서 rental-service는 최소 두 참여 결과를 기다린다.

- book-service: 도서 `UNAVAILABLE` 처리 결과
- member-service: 대여 포인트 적립 결과

권장 상태 저장 단위:

```text
rent_saga_state
├── correlation_id
├── source_event_id
├── member_id
├── item_no
├── book_result: PENDING/SUCCESS/FAILED
├── member_result: PENDING/SUCCESS/FAILED
├── saga_status: STARTED/COMPLETED/COMPENSATING/COMPENSATED/FAILED/NEEDS_MANUAL_REVIEW
├── started_at
└── updated_at
```

RENT 실패 판단 예시:

| book result | member result | 처리 방향 |
|---|---|---|
| FAILED | SUCCESS | rental 대여 취소 + 포인트 차감 보상 command 발행 |
| FAILED | FAILED | rental 대여 취소만 수행 |
| FAILED | PENDING | rental 대여 취소, 이후 member 성공 Result가 늦게 오면 포인트 차감 보상 |
| SUCCESS | FAILED | rental 대여 취소 + 도서 대여 취소 이벤트 발행 |
| PENDING | FAILED | rental 대여 취소, 이후 book 성공 Result가 늦게 오면 도서 대여 취소 이벤트 발행 |
| SUCCESS | SUCCESS | SAGA 완료 |

핵심은 이미 성공이 확인된 참여자만 보상하는 것이다. 아직 결과가 오지 않은 참여자는 실패로 단정하지 않고, 늦게 성공 Result가 도착했을 때 후속 보상을 실행할 수 있게 상태를 남긴다.

### Timeout 보강 검토

`ItemRented` 발행 후 N초 안에 기대한 Result Event가 오지 않는 경우를 처리하려면 rental-service가 기대 결과 목록과 시작 시간을 로컬에 저장해야 한다.

가능한 선택지:

| 방식 | 장점 | 단점 |
|---|---|---|
| timeout 무시 | 구현이 가장 단순함 | Result 누락 시 상태 불일치가 오래 남음 |
| timeout 모니터링만 수행 | 자동 오보상 위험이 낮고 Choreography 성격을 유지하기 쉬움 | 운영 개입이 필요함 |
| timeout을 실패로 간주해 자동 보상 | 자동 복구성이 높음 | 늦게 성공 Result가 도착할 때 역보상 처리가 복잡하고 orchestration 성격이 강해짐 |

권장 방향:

- 1차로는 스케줄러 기반 timeout 모니터링을 도입한다.
- timeout 된 SAGA의 기본 상태는 `NEEDS_MANUAL_REVIEW`로 표시한다.
- timeout만으로 자동 보상을 실행하지 않는다.
- 이미 성공 Result가 확인된 참여자에 대한 자동 보상도 별도 설계 결정 이후에만 도입한다.
- 결과가 없던 참여자가 늦게 성공 Result를 보내면, 현재 SAGA 상태를 보고 필요한 보상을 실행한다.
- 자동 timeout 보상은 명시적인 설계 결정 이후 도입한다.

이 프로젝트는 SAGA orchestration code를 기본적으로 도입하지 않는다는 예외 규칙이 있으므로, timeout 스케줄러는 중앙 오케스트레이터가 아니라 rental-service가 자신이 발행한 흐름을 감시하는 로컬 상태 점검으로 제한한다.

## P0. 보상 멱등성 분리

### 문제

Result Event의 `eventId`를 올바르게 고유화하면, 같은 `correlationId`에 대해 여러 참여 서비스 실패 결과가 각각 도착할 수 있다. 이때 메시지 멱등성만으로는 같은 비즈니스 보상이 여러 번 실행되는 것을 막지 못한다.

예시:

- 대여 흐름에서 book-service 실패
- 같은 대여 흐름에서 member-service 실패
- 두 실패 Result Event가 모두 `EventType.RENT`로 도착
- rental-service가 `cancelRentItem()`을 두 번 시도할 수 있음

### 개선안

메시지 처리 멱등성과 보상 실행 멱등성을 분리한다.

적용 방향:

- `processed:rental-result:{eventId}`: Result Event 메시지 중복 방지
- `compensated:{correlationId}:{eventType}:{participant}:{step}`: 같은 비즈니스 보상 중복 방지
- 보상 유형이 Result의 참여자/단계와 1:1로 매핑되지 않으면 `compensated:{correlationId}:{compensationType}`처럼 명시적인 보상 타입을 키에 포함한다.
- 보상 use case 진입 전 또는 보상 application service 내부에서 보상 실행 여부를 확인한다.
- Aggregate 보상 메서드도 이미 보상된 상태를 안전하게 처리할 수 있도록 상태 체크를 갖춘다.

## P1. 보상 완료 도메인 이벤트 발행

### 문제

`RentalCardService.cancelRentItem()`, `cancelReturnItem()`, `cancelMakeAvailableRental()`은 내부 상태를 되돌리지만 보상 완료 사실을 도메인 이벤트로 발행하지 않는다.

대표 위치:

- `rental-service/src/main/java/com/example/library/rental/application/service/RentalCardService.java`
- `bestbook-service/src/main/java/com/example/library/bestbook/adapter/in/messaging/consumer/BestBookEventConsumer.java`

특히 bestbook-service는 `ItemRented`를 보고 인기 도서 카운트를 증가시키지만, 대여가 보상 취소되어도 카운트를 되돌릴 메시지가 없다.

### 개선안

보상 완료를 표현하는 도메인 이벤트를 추가한다.

후보 이벤트:

- `ItemRentCanceled`
- `ItemReturnCanceled`
- `OverdueClearCanceled`

적용 방향:

- 공용 메시지이므로 `common-events/src/main/java/com/example/library/common/event`에 record로 둔다.
- 각 이벤트는 새 `eventId`와 기존 `correlationId`를 가진다.
- rental-service의 compensation use case가 보상 저장 후 이벤트를 발행한다.
- bestbook-service는 `ItemRentCanceled`를 소비해 rent count를 감소시키거나, 취소 이벤트를 별도 read model 상태로 반영한다.

## P1. Aggregate와 통합 메시지 계약 분리

### 문제

`RentalCard` aggregate가 `common-events` 모듈의 서비스 간 메시지 record를 직접 생성해 반환한다.

대표 위치:

- `rental-service/src/main/java/com/example/library/rental/domain/model/RentalCard.java`
- `common-events/src/main/java/com/example/library/common/event/ItemRented.java`
- `common-events/src/main/java/com/example/library/common/event/ItemReturned.java`
- `common-events/src/main/java/com/example/library/common/event/OverdueCleared.java`

현재 `RentalCard.createItemRentedEvent(...)`, `createItemReturnEvent(...)`, `createOverdueClearedEvent(...)`는 각각 `ItemRented`, `ItemReturned`, `OverdueCleared`를 직접 만든다. 이 record들은 서비스 간 Kafka 메시지 계약이므로, rental 도메인이 통합 메시지 스키마에 결합되어 있다.

이 결합의 영향:

- 공용 메시지 필드 변경이 rental 도메인 aggregate 변경으로 이어진다.
- Kafka 전송 요구사항인 `eventId`, `correlationId`, `occurredAt` 생성 책임이 aggregate 내부로 들어온다.
- 도메인 이벤트와 통합 이벤트 메시지의 목적이 섞인다.
- domain layer가 메시징 계약 모듈을 알아야 하므로 순수 도메인 모델의 독립성이 약해진다.

### 개선안

Aggregate는 순수 도메인 사실 또는 상태 변경 결과만 만들고, 공용 통합 메시지로의 변환은 application service 또는 outbound messaging adapter에서 수행한다.

권장 방향:

- rental-service 내부에 순수 도메인 이벤트를 둔다. 예: `RentalCardEvents.ItemRentedDomainEvent`
- 도메인 이벤트는 비즈니스 의미만 담고 `common-events`와 무관해야 한다.
- `RentalCard`는 공용 `ItemRented` 대신 내부 도메인 이벤트를 생성하거나 반환한다.
- application service 또는 `adapter.out.messaging`에서 도메인 이벤트를 `common-events`의 통합 이벤트로 변환한다.
- `eventId`, `correlationId`, `occurredAt` 같은 메시지 메타데이터는 aggregate가 아니라 application/messaging 경계에서 부여한다.
- 기존 Kafka 계약인 `eventId`와 `correlationId`는 통합 이벤트 변환 시 반드시 유지한다.

예상 구조:

```text
rental-service/
  domain/
    model/
      RentalCard.java
      RentalItemRented.java
      RentalItemReturned.java
      RentalOverdueCleared.java
      RentalCardEvents.java
  application/
    service/
      RentalCardService.java
    port/
      out/
        PublishItemRentedPort.java
  adapter/
    out/
      messaging/
        RentalKafkaEventProducer.java

common-events/
  event/
    ItemRented.java
    ItemReturned.java
    OverdueCleared.java
```

도메인 이벤트 예시:

```java
public final class RentalCardEvents {
    private RentalCardEvents() {
    }

    public record ItemRentedDomainEvent(
        IDName member,
        Item item,
        long point
    ) {
    }

    public record ItemReturnedDomainEvent(
        IDName member,
        Item item,
        long point
    ) {
    }

    public record OverdueClearedDomainEvent(
        IDName member,
        long point
    ) {
    }
}
```

예상 흐름:

```java
RentalItemRented domainEvent = rentalCard.rentItem(item);
saveRentalCardPort.save(rentalCard);
publishItemRentedPort.publish(domainEvent, correlationId);
```

`RentalKafkaEventProducer` 또는 전용 mapper는 `RentalCardEvents.ItemRentedDomainEvent`를 `common-events`의 `ItemRented`로 변환한다.

```java
ItemRented message = new ItemRented(
    UUID.randomUUID().toString(),
    correlationId,
    Instant.now(),
    domainEvent.member(),
    domainEvent.item(),
    domainEvent.point()
);
```

### 변환 시점 의견

이 프로젝트에서는 통합 이벤트 변환 시점을 `adapter.out.messaging`에 두는 방식을 권장한다.

이유:

- `common-events` record는 Kafka 통합 메시지 계약이므로 outbound messaging adapter의 관심사에 가깝다.
- application service는 `correlationId`를 생성하고 유스케이스 흐름을 조율하되, 통합 메시지 스키마 필드 조립에는 덜 결합된다.
- outbound port는 내부 도메인 이벤트와 `correlationId`를 받는 형태로 유지할 수 있다.

권장 port 형태:

```java
public interface PublishItemRentedPort {
    void publish(RentalCardEvents.ItemRentedDomainEvent event, String correlationId);
}
```

대안으로 application service에서 통합 이벤트까지 만들고 port에 전달할 수도 있다. 이 방식은 구현은 단순하지만 application 계층이 `common-events` 스키마와 메시지 메타데이터 생성에 더 강하게 결합된다. 현재 목표가 도메인과 통합 메시지 계약 분리라면 adapter 변환이 더 적절하다.

### Spring 이벤트 기능 검토

Spring Data의 `@DomainEvents`는 aggregate가 Spring Data 어노테이션을 알아야 하므로 이 프로젝트의 순수 domain 원칙과 충돌한다. `RentalCard`에 `@DomainEvents`를 직접 붙이는 방식은 권장하지 않는다.

`ApplicationEventPublisher`는 application service에서 process-local 이벤트를 발행하는 용도로는 사용할 수 있다. 다만 Kafka 통합 이벤트 발행을 Spring application event에 숨기면 흐름 추적이 어려워지고, outbound port를 경유한다는 헥사고널 구조가 흐려질 수 있다.

검토 결론:

- 현재 규모에서는 aggregate가 도메인 이벤트를 반환하고 application service가 outbound port를 호출하는 명시적 흐름을 우선한다.
- Spring `ApplicationEventPublisher`를 쓰려면 application 내부 이벤트 디스패처로 한정하고, Kafka 발행은 결국 `application/port/out`의 publish port를 거치게 한다.
- `@DomainEvents`는 domain의 Spring 결합을 만들기 때문에 사용하지 않는다.

### 주의점

`common-events`는 여러 서비스가 공유하는 계약이므로 계속 유지한다. 다만 이 계약을 aggregate가 직접 생성하지 않게 한다.

도메인 이벤트 record가 여러 서비스에서 공유될 필요가 없다면 rental-service 내부에 둔다. 여러 서비스가 소비해야 하는 외부 계약은 `common-events`에 두되, 변환 책임은 messaging adapter 또는 application 경계에 둔다.

`correlationId`는 하나의 비즈니스 흐름을 묶는 값이므로 application service가 생성하거나 기존 흐름에서 받은 값을 보존한다. `eventId`는 실제 발행되는 통합 메시지마다 새로 생성한다. 즉, 도메인 이벤트에는 `eventId`가 없어도 되며, 통합 이벤트로 변환되는 순간 `eventId`와 `correlationId` 계약을 완성한다.

## P1. Result Event 생성 책임 정리

### 문제

Result Event 생성 코드가 application service 내부에 흩어져 있고, 원본 이벤트 ID 재사용 같은 실수가 반복되기 쉽다.

### 개선안

`common-events`의 Result Event에 정적 팩토리 메서드를 둔다.

예상 형태:

```java
public static EventResult success(
    String sourceEventId,
    String correlationId,
    EventType eventType,
    Participant participant,
    SagaStep step,
    IDName idName,
    Item item,
    long point
) {
    return new EventResult(
        UUID.randomUUID().toString(),
        correlationId,
        sourceEventId,
        Instant.now(),
        eventType,
        participant,
        step,
        true,
        idName,
        item,
        point,
        null
    );
}
```

적용 방향:

- book-service와 member-service는 `new EventResult(...)` 대신 `EventResult.success(...)`, `EventResult.failure(...)`를 사용한다.
- record 접근자는 JavaBean getter 없이 `eventId()`, `correlationId()` 형태를 유지한다.

## P1. application 계층의 config 의존 제거

### 문제

application service가 config 패키지나 Spring 설정 주입에 직접 의존한다.

대표 위치:

- `book-service/src/main/java/com/example/library/book/application/service/BookRentalEventService.java`
- `member-service/src/main/java/com/example/library/member/application/service/MemberEventService.java`

### 개선안

장애 주입이나 테스트용 실패 정책을 application port로 분리한다.

예상 구조:

```text
application/
  port/
    out/
      BookRentalFailurePolicyPort.java
adapter/
  out/
    policy/
      PropertyBookRentalFailurePolicyAdapter.java
config/
  BookFailureProperties.java
```

적용 방향:

- application service는 `BookRentalFailurePolicyPort`만 의존한다.
- adapter/config 쪽에서 properties를 읽어 policy port 구현체를 제공한다.
- member-service의 `@Value` 직접 주입도 같은 방식으로 제거한다.

## P2. rental-service DTO 분리 일관화

### 문제

book/member/bestbook은 application result DTO를 반환하지만, rental-service의 inbound port는 `RentalCard`, `RentItem`, `ReturnItem` 도메인 모델을 직접 반환한다.

대표 위치:

- `rental-service/src/main/java/com/example/library/rental/application/port/in/RentItemUseCase.java`
- `rental-service/src/main/java/com/example/library/rental/application/port/in/RentalCardQueryUseCase.java`
- `rental-service/src/main/java/com/example/library/rental/adapter/in/web/dto/RentalCardResponse.java`

### 개선안

rental-service에도 application result DTO를 추가한다.

후보 DTO:

- `RentalCardResult`
- `RentItemResult`
- `ReturnItemResult`
- `RentalActionResult`

적용 방향:

- use case 반환 타입을 domain model에서 application result로 변경한다.
- web response는 application result만 참조한다.
- persistence adapter와 domain model 구조는 유지한다.

## 권장 적용 순서

1. Consumer 멱등성을 `hasKey -> business -> set`에서 Redis `SETNX` 기반 원자 점유로 변경한다.
2. 정합성이 중요한 Consumer에는 DB 트랜잭션 기반 Idempotent Receiver 처리를 추가한다.
3. `EventResult`에 `sourceEventId`, `participant`, `step`을 추가하고 Result Event의 `eventId`를 새 UUID로 변경한다.
4. book/member 서비스의 Result Event 생성 코드를 정적 팩토리로 통일한다.
5. member-service의 모든 참여 흐름에서 성공/실패 Result Event를 발행한다.
6. rental-service에 `correlationId` 기준 SAGA 상태 저장과 참여 결과 추적을 추가한다.
7. rental-service가 RENT 흐름에서 book 실패와 member 포인트 실패를 구분해 이미 성공한 참여자만 보상하도록 수정한다.
8. rental-service에 `correlationId + eventType + participant + step` 또는 `correlationId + compensationType` 기준 보상 멱등성을 추가한다.
9. SAGA timeout은 먼저 스케줄러 기반 모니터링과 `NEEDS_MANUAL_REVIEW` 상태로 도입하고, 자동 timeout 보상은 별도 설계 결정으로 분리한다.
10. `RentalCard` aggregate가 `common-events` 메시지 record를 직접 생성하지 않도록 내부 도메인 이벤트와 통합 메시지 변환을 분리한다.
11. 보상 완료 이벤트를 `common-events`에 추가하고 rental-service에서 발행한다.
12. bestbook-service가 보상 완료 이벤트를 반영하도록 read model 갱신 로직을 확장한다.
13. application 계층의 config 의존을 port/adapter로 분리한다.
14. rental-service의 use case 반환 타입을 application result DTO로 정리한다.

## 검증 기준

- `./gradlew.bat test`가 통과해야 한다.
- 같은 `eventId` 메시지가 동시에 도착해도 비즈니스 로직은 한 번만 실행되어야 한다.
- 처리 실패 시 processed message record가 남지 않아 Kafka 재전달 또는 재시도가 가능해야 한다.
- DB 상태 변경과 processed message record 저장은 같은 트랜잭션으로 커밋되어야 한다.
- Redis `SETNX` 점유 실패와 이미 처리 완료된 메시지는 로그와 분기에서 구분되어야 한다.
- 모든 SAGA 참여 서비스는 성공/실패 모두 `EventResult`를 발행해야 한다.
- `EventResult`는 `eventId`, `correlationId`, `sourceEventId`, `participant`, `step`을 포함해야 한다.
- rental-service는 같은 RENT 흐름에서 book 결과와 member 결과를 분리해 저장해야 한다.
- rent 흐름에서 book-service 실패 시 rental-service 대여 상태가 보상되어야 한다.
- rent 흐름에서 member-service 실패 시 rental-service 대여 상태와 포인트 보상이 실행되어야 한다.
- book 실패와 member 실패가 동시에 또는 순서가 바뀌어 도착해도 이미 성공한 참여자만 보상되어야 한다.
- SAGA timeout 대상의 기본 상태는 `NEEDS_MANUAL_REVIEW`로 식별되어야 한다.
- 같은 Result Event를 재전송해도 메시지 중복 처리되지 않아야 한다.
- 같은 보상 멱등성 키의 실패 Result Event가 여러 개 도착해도 보상은 한 번만 실행되어야 한다.
- 보상 완료 이벤트를 받은 bestbook-service read model이 정합성 있게 갱신되어야 한다.
- rental domain package에서 `com.example.library.common.event` import가 없어야 한다.
- application 패키지에서 adapter/config 패키지 import가 없어야 한다.
