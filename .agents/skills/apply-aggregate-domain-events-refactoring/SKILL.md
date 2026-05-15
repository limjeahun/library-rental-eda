---
name: apply-aggregate-domain-events-refactoring
description: library-rental-eda에서 rental-service의 RentalCard aggregate-collected Domain Events 구현을 기준 예시로 삼아 member-service와 book-service의 도메인 상태 변경을 aggregate-collected Domain Events 방식으로 리팩터링하거나 리뷰할 때 사용한다. Trigger when adding MemberDomainEvent/BookDomainEvent marker interfaces and record events, moving point/book-status event creation into Member or Book aggregate methods, changing outbound messaging ports to accept service-local domain events plus correlationId, moving common-events conversion into Kafka producer adapters, or checking that reconstituted aggregates do not republish old events. Do not use for bestbook read-model-only updates, unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, SAGA orchestration code, or MapStruct-only mapper changes.
---

# Aggregate-Collected Domain Events Refactoring

## 목적

`rental-service`의 `RentalCard`처럼 aggregate가 자기 상태 변경 과정에서 service-local domain event를 수집하고, application service가 저장 후 이벤트를 꺼내 outbound port로 전달하는 구조를 `member-service`, `book-service`에 적용한다.

이 스킬은 `EventResult` 자체를 도메인 이벤트로 바꾸는 작업이 아니다. `EventResult`는 Kafka로 공유되는 처리 결과 통합 메시지이며, domain event는 각 서비스의 순수 도메인 언어로 표현된 내부 사실이다.

## 필수 기준

작업 전에 다음을 따른다.

1. `AGENTS.md`를 최우선 규칙으로 따른다.
2. 넓은 영향이 있으면 `.agents/skills/architecture-super-agent/scripts/Invoke-ArchitectureScan.ps1 -Root .`를 실행한다.
3. EDA/SAGA 결과 처리나 보상 흐름이 포함되면 `.agents/skills/apply-eda-saga-improvements`와 함께 판단한다.
4. `rental-service/src/main/java/com/example/library/rental/domain/model/RentalCard.java`와 `rental-service/src/main/java/com/example/library/rental/application/service/RentalCardService.java`를 기준 예시로 확인한다.

## 기준 예시

`rental-service`의 현재 목표 패턴:

```text
RentalCard.rentItem(item)
  -> aggregate 상태 변경
  -> registerDomainEvent(new ItemRentedDomainEvent(...))

RentalCardService.rentItem(command)
  -> load aggregate
  -> aggregate behavior 호출
  -> save aggregate
  -> pullRequiredEvent(...)
  -> publish port 호출(domain event + correlationId)

RentalKafkaEventProducer
  -> service-local domain event를 common-events integration message로 변환
  -> eventId, correlationId, occurredAt 같은 Kafka metadata 부여
```

지켜야 할 핵심:

- domain event는 각 서비스의 `domain/event` 아래 top-level `record`로 둔다.
- aggregate는 `common-events`를 import하지 않는다.
- aggregate는 Kafka metadata인 `eventId`, `correlationId`, `participant`, `step`을 알지 않는다.
- application service는 aggregate 상태를 저장한 뒤 `pullDomainEvents()`로 이벤트를 꺼낸다.
- outbound port는 service-local domain event와 `correlationId`를 받는다.
- messaging adapter가 service-local domain event를 shared Kafka contract로 변환한다.
- 순수 domain model에 Spring Data `@DomainEvents`를 붙이지 않는다.

## 고정 적용 패턴

이 스킬을 적용할 때는 다음 계층 규칙을 기본값으로 삼는다. 기존 코드가 다르면 이 구조로 맞춘다.

### 1. 도메인 이벤트 계층 분리

- 도메인 이벤트는 `{service}/domain/event/` 패키지에 둔다.
- 통합 이벤트(`common-events` 모듈)와 완전히 분리한다.
- 도메인 이벤트는 Java `record`로 구현한다.
- 모든 도메인 이벤트는 service-local marker interface를 `implements`한다.

예시:

```text
{service}/domain/event/
├── {Service}DomainEvent.java
├── {Action}DomainEvent.java
└── ...
```

marker interface:

```java
public interface {Service}DomainEvent {
    default Instant occurredAt() {
        return Instant.now();
    }
}
```

domain event record:

```java
public record {Action}DomainEvent(
        RentalMember member,
        RentalItem item,
        long point
) implements {Service}DomainEvent {
}
```

주의:

- `occurredAt()`은 domain event의 내부 발생 시각으로만 사용한다.
- Kafka `eventId`, `correlationId`, `participant`, `step`, retry metadata는 domain event에 넣지 않는다.
- domain event package는 `common-events`, Kafka, Spring messaging 타입을 import하지 않는다.

### 2. Aggregate가 이벤트 발행자

Aggregate는 상태 변경의 유일한 domain event 발행자다.

```java
private final List<{Service}DomainEvent> domainEvents = new ArrayList<>();

private void registerDomainEvent({Service}DomainEvent event) {
    domainEvents.add(event);
}

public List<{Service}DomainEvent> pullDomainEvents() {
    List<{Service}DomainEvent> events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
}
```

상태 변경 메서드는 다음 순서를 지킨다.

1. 검증
2. 상태 변경
3. `registerDomainEvent(...)`

`pull`이라는 이름을 유지한다. 호출자는 이벤트를 가져가고 aggregate 내부 버퍼는 비워져야 한다.

`reconstitute()` 같은 복원 팩토리는 `domainEvents`를 파라미터로 받지 않는다. `final + new ArrayList<>()` 필드 초기화로 DB 복원 aggregate의 이벤트 버퍼가 항상 비어 있게 한다.

### 3. Application Service의 책임

Application service는 다음 순서를 지킨다.

```text
load aggregate
-> aggregate domain method 호출
-> repository save
-> pullDomainEvents()
-> outbound port로 domain event + correlationId 발행
```

성공 이벤트를 통합 이벤트로 직접 만들지 않는다. 성공 처리는 aggregate가 올린 domain event가 있어야만 발행한다.

타입 안전한 helper를 사용한다.

```java
private <T extends {Service}DomainEvent> T pullRequiredEvent(
    {AggregateRoot} aggregate,
    Class<T> eventType
) {
    return aggregate.pullDomainEvents()
        .stream()
        .filter(eventType::isInstance)
        .map(eventType::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Expected domain event was not raised: " + eventType.getSimpleName()
        ));
}
```

실패 Result는 상태 변경이 성공하지 않았으므로 aggregate domain event가 없다. 실패 응답이 필요한 흐름에서는 application-level failure payload나 failure 전용 port method를 사용하되, `common-events` 타입을 application/domain으로 끌어올리지 않는다.

### 4. Outbound Port와 Adapter 책임

- 성공 outbound port method는 domain event를 받는다.
- `correlationId`는 domain event 내부가 아니라 별도 파라미터로 받는다.
- Kafka producer adapter(`out/messaging`)만 domain event와 `common-events` 통합 이벤트 양쪽 형식을 알고 변환한다.

권장 port 형태:

```java
public interface Publish{Service}EventResultPort {
    void publish({Action}DomainEvent event, String correlationId);
}
```

여러 domain event를 한 port에서 처리해야 하면 marker를 받을 수 있지만, adapter에서 `instanceof` 분기 또는 overloaded private mapper로 명시적으로 변환한다.

```java
void publish({Service}DomainEvent event, String correlationId);
```

adapter 책임:

```text
{Service}DomainEvent
-> common-events EventResult 또는 서비스 통합 메시지
-> eventId/correlationId/participant/step/occurredAt metadata 부여
-> Kafka publish
```

이 위치 덕분에 domain은 `common-events`를 모르고, application은 Kafka contract 변환 디테일을 모른다.

### 5. 영속성 안전성

- Mapper가 `toDomain` 시 `domainEvents` 필드를 전달하지 않는다.
- Aggregate의 `reconstitute()` 팩토리는 이벤트 버퍼를 항상 빈 리스트로 시작한다.
- JPA/Mongo entity에는 domain event 버퍼를 저장하지 않는다.
- DB에서 복원된 aggregate가 과거 이벤트를 재발행하지 않도록 테스트한다.

## 적용 대상과 우선순위

우선순위는 `member-service` 다음 `book-service`다.

`bestbook-service`는 event-maintained read model이며 outbound 이벤트를 발행하지 않는다. 사용자가 명시적으로 요청하지 않는 한 이 스킬로 aggregate-collected domain events를 강제하지 않는다.

## member-service 목표 구조

포인트 변화는 회원 aggregate의 도메인 사실로 표현한다.

권장 이벤트:

- `MemberPointSavedDomainEvent`
- `MemberPointUsedDomainEvent`

필요하면 업무 의미가 더 분명한 이름을 사용한다.

- `RentPointSavedDomainEvent`
- `ReturnPointSavedDomainEvent`
- `OverduePointUsedDomainEvent`
- `CompensationPointUsedDomainEvent`

이름 선택 기준:

- 같은 `savePoint()`가 대여/반납 포인트를 모두 처리하고 이벤트 결과의 `EventType`이 필요하면 generic 이벤트에 reason/type을 넣지 말고, application service가 command 맥락으로 Result metadata를 만든다.
- 포인트 변경 자체가 도메인 언어에서 구분되어야 하면 메서드를 `saveRentPoint`, `saveReturnPoint`, `useOverduePoint`, `useCompensationPoint`처럼 더 의도적으로 나누고 이벤트도 구분한다.

권장 구현:

```text
member/domain/event/MemberDomainEvent.java
member/domain/event/MemberPointSavedDomainEvent.java
member/domain/event/MemberPointUsedDomainEvent.java

Member
  -> List<MemberDomainEvent> domainEvents
  -> registerDomainEvent(...)
  -> pullDomainEvents()
  -> savePoint(point) 내부에서 MemberPointSavedDomainEvent 등록
  -> usePoint(point) 내부에서 MemberPointUsedDomainEvent 등록

MemberEventService
  -> load member
  -> member.savePoint/usePoint
  -> save member
  -> pullRequiredEvent 또는 pullEvent
  -> MemberDomainEvent + correlationId로 PublishMemberEventResultPort 호출

MemberKafkaEventProducer
  -> MemberDomainEvent를 common-events EventResult로 변환
  -> eventId/correlationId/participant/step metadata 부여
```

주의:

- 실패 Result Event는 aggregate가 만들 수 없다. 상태 변경이 성공하지 않았기 때문이다.
- 실패 Result Event는 기존처럼 `catch`에서 처리하되, `common-events` 타입을 application/domain으로 올리지 않는다.
- `MemberEventResult` 같은 application DTO가 남아 있다면 성공 port payload가 아니라 adapter 내부 변환 모델 또는 실패 전용 payload로 축소하는 방향을 우선 검토한다.
- domain event가 `EventResult`를 직접 알면 안 된다.
- `handlePointUse`는 보상 command 처리이며 현재 outbound result를 발행하지 않는다. 필요한 경우 domain event 수집은 가능하지만, result 발행을 새로 추가하지 않는다.

## book-service 목표 구조

도서 상태 변화는 도서 aggregate의 도메인 사실로 표현한다.

권장 이벤트:

- `BookMadeAvailableDomainEvent`
- `BookMadeUnavailableDomainEvent`

업무 의미를 더 드러내고 싶으면 다음처럼 구분한다.

- `BookReturnedDomainEvent`
- `BookRentedDomainEvent`
- `BookRentCanceledDomainEvent`
- `BookReturnCanceledDomainEvent`

이름 선택 기준:

- `Book` 도메인 언어가 “대여 가능/불가능 상태 변경” 중심이면 `BookMadeAvailableDomainEvent`, `BookMadeUnavailableDomainEvent`를 사용한다.
- SAGA 이벤트 흐름을 학습 목적으로 더 명확히 드러내려면 rental 맥락의 이벤트 이름을 사용하되, Kafka message 타입과 혼동되지 않게 `DomainEvent` suffix를 유지한다.

권장 구현:

```text
book/domain/event/BookDomainEvent.java
book/domain/event/BookMadeAvailableDomainEvent.java
book/domain/event/BookMadeUnavailableDomainEvent.java

Book
  -> List<BookDomainEvent> domainEvents
  -> registerDomainEvent(...)
  -> pullDomainEvents()
  -> makeAvailable() 내부에서 BookMadeAvailableDomainEvent 등록
  -> makeUnAvailable() 내부에서 BookMadeUnavailableDomainEvent 등록

BookRentalEventService
  -> load book
  -> book.makeAvailable/makeUnAvailable
  -> save book
  -> pullRequiredEvent 또는 pullEvent
  -> BookDomainEvent + correlationId로 PublishBookRentalResultPort 호출

BookKafkaEventProducer
  -> BookDomainEvent를 common-events EventResult로 변환
  -> eventId/correlationId/participant/step metadata 부여
```

주의:

- `BookRentalEventResult` 같은 application DTO가 남아 있다면 성공 port payload가 아니라 adapter 내부 변환 모델 또는 실패 전용 payload로 축소하는 방향을 우선 검토한다.
- 성공 Result 통합 메시지는 adapter가 aggregate-raised domain event를 근거로 만든다.
- 실패 Result는 application service의 `catch`에서 처리하되, `common-events` 타입을 application/domain으로 올리지 않는다.
- cancel 이벤트 처리도 상태가 실제로 변경될 때만 domain event를 남기는 방향이 좋다.

## 리팩터링 절차

한 번에 한 서비스 slice만 끝낸다. 권장 순서:

1. `member-service`
2. `book-service`

절차:

1. `rg -n "DomainEvent|pullDomainEvents|savePoint|usePoint|makeAvailable|makeUnAvailable|publish.*Result" <module> -S`로 현재 상태를 확인한다.
2. `<service>/domain/event` 패키지에 marker interface와 domain event record를 추가한다.
3. aggregate에 `List<...DomainEvent>` 버퍼, `registerDomainEvent`, `pullDomainEvents`를 추가한다.
4. aggregate 상태 변경 메서드가 실제 상태 변경 후 domain event를 등록하게 한다.
5. application service의 private 상태 변경 helper가 저장된 aggregate를 반환하게 바꾼다.
6. application service가 저장 후 `pullRequiredEvent` 또는 `pullEvent`로 이벤트를 꺼낸다.
7. outbound port signature를 domain event + `correlationId`로 바꾼다.
8. 성공 Result 통합 메시지는 messaging adapter에서 domain event payload와 command/correlation metadata를 조합해 만든다.
9. 실패 Result는 기존 catch 흐름에서 유지하되 application/domain이 `common-events` 타입을 직접 만들지 않게 한다.
10. messaging producer가 domain event를 common-events `EventResult`로 변환하게 한다.
11. mapper `toDomain`/`reconstitute` 경로가 domainEvents를 전달하지 않는지 확인한다.
12. domain package가 `com.example.library.common.event`를 import하지 않는지 확인한다.

## 구현 가드레일

- `common-events` record를 domain event로 사용하지 않는다.
- `application/dto` result를 domain event로 사용하지 않는다.
- domain event record 생성자에 `eventId`, `correlationId`, `Participant`, `SagaStep` 같은 Kafka/SAGA metadata를 넣지 않는다.
- marker interface의 `occurredAt()` 외에 통합 메시지 전용 timestamp 필드를 domain event record 생성자에 추가하지 않는다.
- aggregate가 outbound port나 producer를 호출하지 않는다.
- application service가 다른 inbound use case를 내부 협력 객체처럼 호출하지 않는다.
- application service가 성공 `EventResult` common-events record를 직접 생성하지 않는다.
- outbound messaging port의 성공 method는 service-local domain event를 받게 한다.
- `correlationId`는 port method 파라미터로 전달하고 domain event에 저장하지 않는다.
- Kafka producer adapter만 service-local domain event와 `common-events` 양쪽을 import한다.
- 새 `*Processor`, `*Manager`, `*HelperService` Spring bean을 만들지 않는다.
- 공통화는 같은 클래스 private helper 또는 service-local domain model behavior 안에서만 제한적으로 한다.
- idempotency 처리 순서를 바꿀 때는 기존 처리 실패 재시도 의미를 보존한다.
- no-op 보상 또는 이미 반영된 상태에서 이벤트를 중복 등록하지 않도록 테스트한다.
- `reconstitute()`/mapper 복원 경로에서 domainEvents를 주입하거나 복원하지 않는다.
- entity mapper가 과거 domain event를 재발행할 수 있는 필드를 저장/복원하지 않는다.

## 테스트 기준

각 모듈에 다음 테스트를 추가하거나 수정한다.

member-service:

- `Member.savePoint`가 `MemberPointSavedDomainEvent`를 등록한다.
- `Member.usePoint`가 `MemberPointUsedDomainEvent`를 등록한다.
- `pullDomainEvents()`는 반환 후 내부 버퍼를 비운다.
- `Member.reconstitute()`로 복원한 aggregate의 `pullDomainEvents()`는 빈 리스트를 반환한다.
- `MemberEventService` 성공 흐름은 aggregate event를 pull한 뒤 `PublishMemberEventResultPort`에 domain event와 correlationId를 전달한다.
- `MemberKafkaEventProducer`는 domain event를 common-events `EventResult`로 변환한다.
- domain/outbound port 실패 시 failure result를 발행한다.

book-service:

- `Book.makeAvailable`이 `BookMadeAvailableDomainEvent`를 등록한다.
- `Book.makeUnAvailable`이 `BookMadeUnavailableDomainEvent`를 등록한다.
- `pullDomainEvents()`는 반환 후 내부 버퍼를 비운다.
- `Book.reconstitute()`로 복원한 aggregate의 `pullDomainEvents()`는 빈 리스트를 반환한다.
- `BookRentalEventService` 성공 흐름은 aggregate event를 pull한 뒤 `PublishBookRentalResultPort`에 domain event와 correlationId를 전달한다.
- `BookKafkaEventProducer`는 domain event를 common-events `EventResult`로 변환한다.
- domain/outbound port 실패 시 failure result를 발행한다.

권장 검증:

```powershell
.\gradlew.bat :member-service:test --tests com.example.library.member.domain.model.MemberTest
.\gradlew.bat :member-service:test --tests com.example.library.member.application.service.MemberEventServiceTest
.\gradlew.bat :member-service:test --tests com.example.library.member.architecture.HexagonalArchitectureTest

.\gradlew.bat :book-service:test --tests com.example.library.book.domain.model.BookTest
.\gradlew.bat :book-service:test --tests com.example.library.book.application.service.BookRentalEventServiceTest
.\gradlew.bat :book-service:test --tests com.example.library.book.architecture.HexagonalArchitectureTest
```

테스트 클래스명이 아직 없으면 해당 모듈의 기존 테스트 구조를 따르며 새로 만든다.

## 완료 기준

- `member-service`와 `book-service` domain model이 service-local domain event를 수집한다.
- application service가 성공 이벤트/결과의 핵심 payload를 aggregate-raised domain event에서 가져와 outbound port에 domain event + correlationId로 전달한다.
- Kafka producer adapter가 domain event를 common-events 통합 이벤트로 변환한다.
- 실패 Result Event는 application service에서 유지하되 `common-events` 타입은 adapter 경계 밖으로 새지 않는다.
- domain package가 Spring, Kafka, JPA/Mongo, Redis, web, adapter, config, `common-events`에 의존하지 않는다.
- `EventResult`, `BookRentalEventResult`, `MemberEventResult`와 service-local domain event의 역할이 섞이지 않는다.
- mapper/reconstitute 복원 경로에서 domainEvents가 비어 있어 과거 이벤트가 재발행되지 않는다.
- 관련 unit test와 `HexagonalArchitectureTest`가 통과한다.
