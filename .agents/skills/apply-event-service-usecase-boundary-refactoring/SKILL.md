---
name: apply-event-service-usecase-boundary-refactoring
description: library-rental-eda에서 application/service의 *EventService 또는 *ResultService가 같은 모듈의 application/port/in UseCase를 내부 협력 객체처럼 호출하는 구조를 제거하거나 리뷰할 때 사용한다. Trigger when refactoring BookRentalEventService calling MakeAvailableBookUseCase/MakeUnavailableBookUseCase, MemberEventService calling SavePointUseCase/UsePointUseCase, RentalResultService calling CompensationUseCase, or any Event/Result application service that should orchestrate with outbound ports, domain models, and optional internal application processors instead of re-entering inbound ports. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, SAGA orchestration code, or MapStruct-only mapper changes.
---

# Event Service UseCase Boundary Refactoring

## 목적

이 스킬은 `library-rental-eda`에서 application service 내부가 같은 모듈의 inbound use case port를 다시 호출하는 구조를 정리한다. Kafka inbound event/result use case는 자기 유스케이스를 직접 완결하거나 내부 application processor를 사용해야 하며, 다른 inbound port를 내부 서비스 API처럼 사용하지 않는다.

## 필수 읽기

작업 전에 다음을 확인한다.

1. `AGENTS.md`를 최우선 규칙으로 따른다.
2. 넓은 영향이 있으면 `.agents/skills/architecture-super-agent/scripts/Invoke-ArchitectureScan.ps1 -Root .`를 실행한다.
3. EDA/SAGA 결과 처리나 보상이 포함되면 `docs/hexagonal-eda-saga-review-improvements.md`를 참고한다.
4. 계층 경계 판단이 애매하면 `docs/architecture-rule-eda.md`를 참고한다.

## 문제 패턴

다음 구조를 제거 대상으로 본다.

```text
adapter/in/messaging/consumer
  -> application/service/*EventService
    -> application/port/in/OtherUseCase
      -> application/service/OtherService
```

대표 현재 후보:

- `BookRentalEventService`가 `MakeAvailableBookUseCase`, `MakeUnavailableBookUseCase`를 호출
- `MemberEventService`가 `SavePointUseCase`, `UsePointUseCase`를 호출
- `RentalResultService`가 `CompensationUseCase`를 호출

inbound port는 외부 adapter가 application으로 들어오는 진입 계약이다. application service 내부 재사용을 위해 inbound port를 호출하면 use case 경계가 흐려지고 트랜잭션, 멱등성, 이벤트 발행 책임이 겹치기 쉽다.

## 목표 구조

Event/Result service는 다음 중 하나로 정리한다.

### 방식 A. Event service가 직접 유스케이스 완결

Event service가 필요한 outbound port와 domain model을 직접 사용한다.

```text
MemberEventService
  -> MessageIdempotencyPort
  -> LoadMemberByIdNamePort
  -> Member.savePoint/usePoint
  -> SaveMemberPort
  -> PublishMemberEventResultPort
```

작업이 한 서비스 안에서만 쓰이고 중복이 작으면 이 방식을 우선한다.

### 방식 B. 내부 application processor로 공통화

Web/API use case와 Event use case가 동일한 application 로직을 공유해야 하면 inbound port가 아니라 내부 processor를 만든다.

```text
MemberService
  -> MemberPointProcessor

MemberEventService
  -> MemberPointProcessor
```

processor는 `application/service`에 두되 inbound port를 구현하지 않는다. 이름은 `MemberPointProcessor`, `RentalCompensationProcessor`, `BookAvailabilityProcessor`처럼 내부 협력 책임을 드러낸다.

## 적용 규칙

- `*EventService`와 `*ResultService`는 자기 자신이 구현하는 inbound port만 import한다.
- Event/Result service는 다른 `application.port.in.*UseCase`를 필드로 주입받지 않는다.
- 공통 로직이 필요하면 inbound port 대신 package-private 또는 public application processor를 둔다.
- processor는 domain model과 outbound port를 사용해 application 내부 로직만 수행한다.
- adapter, config, web DTO, Kafka consumer를 application service나 processor에 import하지 않는다.
- Kafka shared message는 adapter 또는 event service 경계에서 application/domain 값으로 풀어낸다.
- 보상 흐름에서는 message idempotency와 compensation idempotency를 유지한다.
- 기존 테스트가 `OtherUseCase` mock으로 실패를 유도했다면, 리팩터링 후에는 processor 또는 outbound port mock으로 실패를 유도한다.

## 서비스별 권장 방향

### member-service

`MemberEventService`에서 `SavePointUseCase`와 `UsePointUseCase` 의존을 제거한다.

권장 우선순위:

1. `MemberPointProcessor`를 만든다.
2. `MemberService.savePoint/usePoint`와 `MemberEventService`가 processor를 사용하게 한다.
3. processor는 `LoadMemberByIdNamePort`, `SaveMemberPort`, `MemberIdentity`, `Member` domain behavior를 사용한다.
4. `MemberEventService` 테스트는 processor 또는 outbound port 실패로 failure `EventResult` 발행을 검증한다.

### book-service

`BookRentalEventService`에서 `MakeAvailableBookUseCase`, `MakeUnavailableBookUseCase` 의존을 제거한다.

권장 우선순위:

1. 중복이 작으면 `LoadBookPort`, `SaveBookPort`, `Book` domain behavior를 직접 사용한다.
2. Web/API service와 공유가 필요하면 `BookAvailabilityProcessor`를 만든다.
3. `BookService.makeAvailable/makeUnavailable`과 `BookRentalEventService`가 processor를 사용하게 한다.
4. Result Event 성공/실패 발행과 message idempotency는 `BookRentalEventService`에 남긴다.

### rental-service

`RentalResultService`에서 `CompensationUseCase` 의존을 제거한다.

권장 우선순위:

1. `RentalCompensationProcessor`를 만든다.
2. `RentalCardService`의 compensation 메서드와 `RentalResultService`가 processor를 사용하게 한다.
3. processor는 `LoadRentalCardPort`, `SaveRentalCardPort`, `CompensationIdempotencyPort`, 보상 완료 event publish port를 사용한다.
4. `RentalResultService`는 result 해석, saga state 기록, 보상 필요 여부 판단까지만 담당하고 실제 보상 상태 변경은 processor에 위임한다.

## 리팩터링 절차

1. `rg "application\\.port\\.in|UseCase" -n --glob "*EventService.java" --glob "*ResultService.java"`로 후보를 찾는다.
2. 한 번에 한 서비스 slice만 바꾼다. 권장 순서: member-service, book-service, rental-service.
3. 현재 inbound port 메서드가 하는 domain/outbound port 호출을 service 내부 또는 processor로 옮긴다.
4. 기존 Web/API use case가 동일 로직을 쓰면 processor로 공통화하고, public API 반환 DTO 변환은 기존 use case service에 남긴다.
5. Event/Result service의 import에서 자기 inbound port 외 다른 `application.port.in` 의존이 사라졌는지 확인한다.
6. 관련 unit test를 수정하거나 추가한다.
7. 관련 모듈의 `HexagonalArchitectureTest`와 targeted tests를 실행한다.

## 테스트 기준

각 서비스에서 다음을 확인한다.

- Event/Result service가 다른 inbound use case mock 없이 테스트된다.
- 멱등 처리 실패 시 business logic을 실행하지 않는다.
- domain/outbound port 또는 processor가 실패하면 실패 Result Event가 발행된다.
- 성공 흐름의 Result Event metadata가 유지된다.
- 보상 흐름은 기존 compensation idempotency key를 유지한다.

권장 검증:

```powershell
.\gradlew.bat :member-service:test --tests com.example.library.member.architecture.HexagonalArchitectureTest
.\gradlew.bat :book-service:test --tests com.example.library.book.architecture.HexagonalArchitectureTest
.\gradlew.bat :rental-service:test --tests com.example.library.rental.architecture.HexagonalArchitectureTest
.\gradlew.bat compileJava compileTestJava
```

작업 범위가 한 모듈이면 해당 모듈 test를 우선한다. 여러 모듈을 연달아 바꾸면 `.\gradlew.bat test`를 검토한다.

## 완료 기준

- `*EventService`/`*ResultService`가 자기 inbound port 외 다른 `application.port.in` use case를 주입받지 않는다.
- 공통 application 로직은 inbound port가 아니라 processor 또는 private helper로 재사용된다.
- adapter/config/web/persistence 구현이 application service에 새로 유입되지 않는다.
- 테스트는 설정 flag나 다른 inbound use case mock 대신 processor/outbound port/domain 실패로 실패 흐름을 검증한다.
