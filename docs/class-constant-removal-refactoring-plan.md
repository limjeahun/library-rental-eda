# 클래스 상수 제거 리팩터링 개선안

## 목적

이 문서는 application service와 Kafka consumer 등에 흩어진 class-level constant를 제거하고, 값의 성격에 맞는 타입과 책임 위치로 옮기기 위한 개선안이다.

특히 다음과 같은 형태를 제거 대상으로 본다.

```java
private static final long RENT_POINT = 10L;
private static final long RETURN_POINT = 10L;
private static final String RENTAL_RENT_CANCEL = "RENTAL_RENT_CANCEL";
private static final String RENTAL_RETURN_CANCEL = "RENTAL_RETURN_CANCEL";
private static final String MEMBER_RENT_POINT_USE = "MEMBER_RENT_POINT_USE";
```

이런 상수는 단순 중복 제거처럼 보이지만 실제로는 서로 다른 책임을 한 클래스에 섞는다.

- 도메인 정책값: `RENT_POINT`, `RETURN_POINT`
- 보상 멱등성 키: `RENTAL_RENT_CANCEL`, `RENTAL_RETURN_CANCEL`
- cross-service command 사유: `RENT_COMPENSATION`, `RETURN_COMPENSATION`
- 메시지 처리 식별자: `SERVICE_NAME`, `"ItemRented"`, `"EventResult"`
- 기술 설정값: `PROCESSING_TTL`

리팩터링의 목표는 상수를 다른 유틸 클래스로 모으는 것이 아니다. 각 값이 표현하는 개념을 타입으로 만들고, 그 타입을 올바른 계층에 배치하는 것이다.

## 현재 문제

### 1. 응집도 문제

`RentalCardService`는 대여, 반납, 연체 해제 use case를 조율하는 application service다.

하지만 현재 클래스 안에는 다음 책임이 함께 있다.

- 대여/반납 포인트 정책
- 보상 이벤트 발행 흐름
- 보상 멱등성 타입 문자열
- 회원 포인트 차감 command 사유 문자열

Application service가 도메인 정책과 프로토콜 vocabulary까지 정의하면 변경 이유가 너무 많아진다.

### 2. 매직 스트링 문제

보상 키와 메시지 타입이 문자열이면 오타를 컴파일러가 잡을 수 없다.

```text
compensationIdempotencyPort.markCompensated(correlationId, "RENTAL_RENT_CANCEL");
```

이 값은 finite concept이다. 따라서 문자열 상수보다 enum 또는 value object가 더 적합하다.

### 3. 도메인 정책 누수

`RENT_POINT = 10L`은 기술 값이 아니라 비즈니스 규칙이다.

“대여하면 10포인트 적립한다”, “반납하면 10포인트 적립한다”는 대여 도메인의 정책이다. Application service의 class constant가 아니라 domain policy, domain enum, domain value object, 또는 도메인 모델의 행동으로 표현해야 한다.

### 4. 프로토콜 키의 책임 위치 불명확

`SERVICE_NAME`, `"ItemRented"`, `"PointUseCommand"` 같은 값은 메시지 멱등성 저장에 사용된다.

이 값이 persistence audit 용도라면 adapter 또는 config에 가까운 값이다. 반대로 서비스 간 메시지 프로토콜의 일부라면 `common-events`의 typed protocol enum이 적합하다. Application service가 직접 문자열로 들고 있으면 어느 쪽 책임인지 모호해진다.

## 현재 코드 기준 상수 분류

현재 검색 기준:

```powershell
rg "private static final|public static final|static final" -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
```

### 제거 우선순위 P0

| 위치 | 현재 값 | 문제 | 권장 방향 |
|------|---------|------|-----------|
| `RentalCardService` | `RENT_POINT`, `RETURN_POINT` | 도메인 정책값이 application service에 있음 | `RentalPointPolicy` 같은 domain enum/policy로 이동 |
| `RentalCardService` | `RENTAL_RENT_CANCEL`, `RENTAL_RETURN_CANCEL`, `RENTAL_OVERDUE_CLEAR_CANCEL` | 보상 멱등성 키가 문자열 | `RentalCompensationType` enum/value object로 변경 |
| `RentalCardService` | `MEMBER_RENT_POINT_USE`, `MEMBER_RETURN_POINT_USE` | downstream 보상 command 종류가 문자열 | `PointUseReason` 또는 `PointCompensationReason` typed enum으로 변경 |
| `RentalCardService` | `"RENT_COMPENSATION"`, `"RETURN_COMPENSATION"` | command reason이 magic string | command reason 타입으로 변경 |

### 제거 우선순위 P1

| 위치 | 현재 값 | 문제 | 권장 방향 |
|------|---------|------|-----------|
| `BookRentalEventService` | `SERVICE_NAME` | idempotency 저장용 서비스명 문자열이 application service에 있음 | port에서 제거하거나 adapter/config에서 주입 |
| `MemberEventService` | `SERVICE_NAME` | 동일 | port에서 제거하거나 adapter/config에서 주입 |
| `RentalResultService` | `SERVICE_NAME` | 동일 | port에서 제거하거나 adapter/config에서 주입 |
| `BestBookService` | `SERVICE_NAME` | 동일 | port에서 제거하거나 adapter/config에서 주입 |
| 여러 application service | `"ItemRented"`, `"ItemReturned"`, `"EventResult"` | 메시지 타입 magic string | typed message type 또는 메시지 class 기반 adapter-local 변환 |

### 제거 우선순위 P2

| 위치 | 현재 값 | 문제 | 권장 방향 |
|------|---------|------|-----------|
| Kafka consumers | `PROCESSING_TTL = Duration.ofMinutes(10)` | technical runtime value가 consumer class constant | `config` properties로 이동 |
| `RentalCard` | `MAX_RENTAL_COUNT = 5` | 도메인 정책값이 aggregate 내부 상수 | domain policy 타입으로 분리할지 판단 |
| `RentItem` | `RENTAL_DAYS = 14` | 대여 기간 정책값이 child model 내부 상수 | domain policy 타입으로 분리할지 판단 |

P2의 domain model 내부 상수는 application service 상수보다 위험도가 낮다. 그래도 “정책값을 타입으로 표현한다”는 기준을 강하게 적용한다면 domain policy object로 분리한다.

## 목표 구조

### Domain Policy

대여/반납 포인트, 대여 가능 수, 대여 기간 같은 값은 domain policy로 표현한다.

예상 위치:

```text
rental-service
└── src/main/java/com/example/library/rental/domain/model
    ├── RentalPointPolicy.java
    ├── RentalLimitPolicy.java
    └── RentalPeriodPolicy.java
```

프로젝트 구조를 크게 늘리지 않기 위해 기본은 `domain/model`에 둔다. 별도 `domain/policy` 패키지는 AGENTS.md의 target structure를 바꾸는 일이므로, 사용자가 명시적으로 원할 때만 검토한다.

예시:

```java
public enum RentalPointPolicy {
    RENT(10L),
    RETURN(10L);

    private final long point;

    RentalPointPolicy(long point) {
        this.point = point;
    }

    public long point() {
        return point;
    }
}
```

Application service는 숫자를 알지 않고 의미 있는 정책 타입을 사용한다.

```java
long point = RentalPointPolicy.RENT.point();
```

더 엄격하게는 domain event 생성도 정책 메서드 뒤로 숨긴다.

```java
RentalCardEvents.ItemRentedDomainEvent event = rentalCard.rentItem(item);
```

이 경우 aggregate가 정책을 적용하고 application service는 결과만 발행한다.

### Compensation Type

보상 멱등성 키는 문자열이 아니라 typed enum으로 표현한다.

예상 위치:

```text
rental-service
└── src/main/java/com/example/library/rental/domain/model
    └── RentalCompensationType.java
```

예시:

```java
public enum RentalCompensationType {
    RENT_CANCEL,
    RETURN_CANCEL,
    OVERDUE_CLEAR_CANCEL,
    RENT_POINT_USE,
    RETURN_POINT_USE
}
```

Outbound port는 문자열 대신 enum을 받는다.

```java
public interface CompensationIdempotencyPort {
    boolean markCompensated(String correlationId, RentalCompensationType compensationType);
}
```

Persistence adapter에서만 DB 저장용 문자열로 변환한다.

```text
compensationType.name()
```

DB 컬럼이 문자열이어도 application service는 문자열 키를 알 필요가 없다.

### Point Command Reason

회원 서비스로 발행되는 point use command의 reason이 프로토콜 의미를 갖는다면 `common-events`에 shared enum으로 둔다.

예상 위치:

```text
common-events
└── src/main/java/com/example/library/common/event
    └── PointUseReason.java
```

예시:

```java
public enum PointUseReason {
    RENT_COMPENSATION,
    RETURN_COMPENSATION,
    OVERDUE_CLEAR
}
```

`PointUseCommand`의 `reason` 필드는 `String`에서 `PointUseReason`으로 변경한다.

```java
public record PointUseCommand(
    String eventId,
    String correlationId,
    Instant occurredAt,
    String memberId,
    String memberName,
    long point,
    PointUseReason reason
) {
}
```

서비스 내부 command도 같은 타입을 사용한다.

```java
public record PointUseCommandRequest(
    String correlationId,
    RentalMember idName,
    long point,
    PointUseReason reason
) {
}
```

### Message Idempotency Identity

`SERVICE_NAME`은 application service에서 제거한다.

권장 1안:

서비스별 idempotency table/store가 분리되어 있다면 `serviceName` 파라미터 자체를 port에서 제거한다.

```java
public interface MessageIdempotencyPort {
    boolean markProcessed(String eventId, String correlationId, InboundMessageType messageType);
}
```

Adapter가 자기 서비스의 저장소에 기록하므로 application service가 `book-service`, `member-service` 같은 문자열을 넘길 이유가 없다.

권장 2안:

DB audit을 위해 service name이 반드시 필요하다면 `config` properties 또는 adapter-local provider로 둔다.

```text
book-service
└── config
    └── ServiceIdentityProperties.java
```

Application service는 여전히 service name을 알지 않는다.

### Inbound Message Type

`"ItemRented"`, `"EventResult"` 같은 문자열도 제거한다.

선택지는 두 가지다.

1. 메시지 record class에서 adapter/persistence 경계에서 이름을 도출한다.
2. 저장/감사 의미가 중요한 경우 typed enum을 사용한다.

예시:

```java
public enum InboundMessageType {
    ITEM_RENTED,
    ITEM_RETURNED,
    OVERDUE_CLEARED,
    POINT_USE_COMMAND,
    EVENT_RESULT,
    ITEM_RENT_CANCELED,
    ITEM_RETURN_CANCELED
}
```

이 enum이 공유 메시지 프로토콜과 강하게 연결된다면 `common-events`에 두고, 서비스 로컬 audit 구분일 뿐이면 각 서비스 application/domain 쪽에 둔다.

### Processing TTL

Kafka consumer의 Redis processing lock TTL은 비즈니스 정책이 아니라 기술 설정이다.

현재:

```java
private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);
```

개선:

```text
config
└── KafkaConsumerProcessingProperties.java
```

```java
@ConfigurationProperties(prefix = "library.kafka.consumer.processing")
public record KafkaConsumerProcessingProperties(Duration ttl) {
}
```

consumer는 properties를 주입받아 사용한다.

## 적용 순서

### 1단계: RentalCardService의 P0 상수 제거

가장 먼저 `RentalCardService`의 도메인 정책값과 보상 키를 제거한다.

작업:

1. `RentalPointPolicy`를 추가한다.
2. `RentalCompensationType`을 추가한다.
3. `PointUseReason`을 추가한다.
4. `CompensationIdempotencyPort` 시그니처를 `String compensationType`에서 `RentalCompensationType`으로 변경한다.
5. `CompensationIdempotencyPersistenceAdapter`에서 `compensationType.name()`으로 저장한다.
6. `PointUseCommandRequest`와 `PointUseCommand`의 reason을 typed enum으로 바꾼다.
7. `RentalCardService`에서 class constants와 magic string reason을 제거한다.
8. 관련 producer/consumer와 테스트를 함께 수정한다.

검증:

```powershell
.\gradlew.bat :common-events:test :rental-service:test --tests com.example.library.rental.domain.model.RentalCardTest --tests com.example.library.rental.application.dto.RentalSagaStateTest
.\gradlew.bat compileJava compileTestJava
```

### 2단계: 메시지 멱등성 service name 제거

`SERVICE_NAME` 문자열을 application service에서 제거한다.

작업:

1. 각 서비스의 `MessageIdempotencyPort`에서 `serviceName` 파라미터 제거를 우선 검토한다.
2. persistence adapter가 service-local table/document를 사용한다면 service name 저장을 제거하거나 adapter-local 값으로 처리한다.
3. audit 때문에 필요하면 `config` properties로 이동한다.
4. application service의 `SERVICE_NAME` 상수를 삭제한다.

대상:

- `book-service/application/service/BookRentalEventService`
- `member-service/application/service/MemberEventService`
- `rental-service/application/service/RentalResultService`
- `bestbook-service/application/service/BestBookService`

검증:

```powershell
.\gradlew.bat :book-service:test :member-service:test :rental-service:test :bestbook-service:test
.\gradlew.bat compileJava compileTestJava
```

### 3단계: 메시지 타입 문자열 제거

`markProcessed(..., "ItemRented")` 같은 문자열을 typed message type 또는 adapter-local derivation으로 바꾼다.

작업:

1. `InboundMessageType`을 shared enum으로 둘지 service-local enum으로 둘지 결정한다.
2. `MessageIdempotencyPort` 시그니처를 `String messageType`에서 typed enum 또는 value object로 바꾼다.
3. persistence adapter에서만 DB/Mongo 저장 문자열로 변환한다.
4. 모든 application service의 메시지 타입 문자열을 제거한다.

검증:

```powershell
rg "\"ItemRented\"|\"ItemReturned\"|\"EventResult\"|\"PointUseCommand\"" -n book-service/src/main member-service/src/main rental-service/src/main bestbook-service/src/main
.\gradlew.bat compileJava compileTestJava
```

### 4단계: Consumer TTL 설정화

Kafka consumer의 `PROCESSING_TTL` class constant를 config property로 이동한다.

작업:

1. 각 서비스 또는 공통 config 방식으로 `KafkaConsumerProcessingProperties`를 추가한다.
2. consumer가 properties를 생성자 주입받도록 변경한다.
3. `application.yml`에 기본 TTL 값을 명시한다.
4. consumer class의 `PROCESSING_TTL` 상수를 제거한다.

검증:

```powershell
.\gradlew.bat compileJava compileTestJava
```

### 5단계: Domain model 내부 정책 상수 검토

`RentalCard.MAX_RENTAL_COUNT`, `RentItem.RENTAL_DAYS`는 application service 상수보다 덜 나쁘지만 여전히 정책값이다.

작업:

1. `RentalLimitPolicy`, `RentalPeriodPolicy` 같은 domain policy 타입으로 분리할지 결정한다.
2. aggregate 또는 child model이 policy 타입을 통해 규칙을 적용하도록 변경한다.
3. 테스트에서 정책값이 명시적으로 검증되는지 확인한다.

검증:

```powershell
.\gradlew.bat :rental-service:test --tests com.example.library.rental.domain.model.RentalCardTest
```

## Before / After 예시

### 보상 멱등성 키

변경 전:

```text
if (!compensationIdempotencyPort.markCompensated(correlationId, RENTAL_RENT_CANCEL)) {
    return;
}
```

변경 후:

```text
if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL)) {
    return;
}
```

### 포인트 정책

변경 전:

```text
saveRentalSagaStatePort.save(RentalSagaState.startRent(correlationId, idName, item, RENT_POINT));
```

변경 후:

```text
long point = RentalPointPolicy.RENT.point();
saveRentalSagaStatePort.save(RentalSagaState.startRent(correlationId, idName, item, point));
```

### PointUseCommand reason

변경 전:

```text
createPointUseCommand(correlationId, idName, RENT_POINT, "RENT_COMPENSATION");
```

변경 후:

```text
createPointUseCommand(correlationId, idName, RentalPointPolicy.RENT.point(), PointUseReason.RENT_COMPENSATION);
```

## AGENTS.md와의 관계

`AGENTS.md`에는 이미 다음 방향이 추가되어 있다.

- application service/use case service에 도메인 정책값, 보상 키, 멱등성 키, 이벤트/커맨드 타입 문자열을 class constant로 두지 않는다.
- 도메인 정책값은 domain model, domain policy, domain VO, domain enum 쪽으로 이동한다.
- magic string은 string constant로 옮기지 말고 enum 또는 value object로 표현한다.
- runtime-configurable technical value는 `config` properties로 이동한다.

이 문서는 그 규칙을 실제 코드에 적용하기 위한 작업 계획이다.

## 하지 않는 것

이 리팩터링은 다음을 도입하지 않는다.

- Outbox pattern
- DLQ, DLT, dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- Direct service-to-service HTTP calls

서비스 간 통신 방식은 기존처럼 Kafka event/command/result 흐름을 유지한다.

## 리뷰 체크리스트

- Application service에 도메인 정책값 class constant가 없다.
- Application service에 보상/멱등성 키 문자열 class constant가 없다.
- finite concept은 문자열이 아니라 enum 또는 value object로 표현된다.
- DB나 Mongo에 문자열로 저장해야 하는 값은 adapter에서만 `.name()` 또는 명시적 persistence value로 변환한다.
- `common-events`에 넣은 enum은 실제로 서비스 간 메시지 프로토콜에 필요한 타입이다.
- 서비스 로컬 개념은 `common-events`로 올리지 않는다.
- Consumer TTL 같은 기술값은 `config` properties로 이동한다.
- Domain model 내부 정책값은 aggregate에 남길지 domain policy로 분리할지 명시적으로 결정한다.
- `rg "private static final|public static final|static final"` 결과를 보고 허용 상수와 제거 대상 상수를 구분했다.

## 최종 확인 명령

```powershell
rg "private static final|public static final|static final" -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
rg "\"RENTAL_RENT_CANCEL\"|\"RENTAL_RETURN_CANCEL\"|\"MEMBER_RENT_POINT_USE\"|\"RENT_COMPENSATION\"|\"ItemRented\"|\"EventResult\"" -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
.\gradlew.bat compileJava compileTestJava
```

전체 테스트는 가능하면 실행하되, 현재 개발 환경에서는 `rental-service`의 MariaDB 의존 persistence test 실패와 이번 리팩터링 실패를 구분해야 한다.
