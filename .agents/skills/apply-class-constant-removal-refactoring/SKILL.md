---
name: apply-class-constant-removal-refactoring
description: library-rental-eda에서 docs/class-constant-removal-refactoring-plan.md의 클래스 상수 제거 개선안을 적용하거나 리뷰할 때 사용한다. Use when removing or reviewing class-level constants and magic strings in application services, use case services, Kafka consumers, or messaging/idempotency flows, especially constants like RENT_POINT, RETURN_POINT, RENTAL_RENT_CANCEL, MEMBER_RENT_POINT_USE, SERVICE_NAME, PROCESSING_TTL, message type strings such as "ItemRented" or "EventResult", compensation/idempotency keys, point command reasons, and domain policy values. Convert these to domain policy enums/value objects, typed compensation/message enums, config properties, or adapter-local conversion. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# 클래스 상수 제거 리팩터링 적용

## 목적

이 스킬은 `library-rental-eda`에서 application service나 Kafka consumer에 박힌 class-level constant, magic string, domain policy value, compensation/idempotency key를 값의 의미에 맞는 타입과 계층으로 옮길 때 사용한다.

목표는 상수를 `Constants` 클래스로 모으는 것이 아니다. 서비스 클래스가 use case 조율만 담당하게 하고, 도메인 정책과 프로토콜 vocabulary를 typed enum, value object, domain policy, 또는 config properties로 분리한다.

## 필수 읽기

코드를 변경하기 전에 다음 문서를 순서대로 읽는다.

1. `AGENTS.md`를 먼저 읽고 최우선 규칙으로 따른다.
2. `docs/class-constant-removal-refactoring-plan.md`를 이 개선안의 source of truth로 삼는다.
3. 보상 멱등성, 메시지 멱등성, result event 흐름을 건드리면 `docs/hexagonal-eda-saga-review-improvements.md`도 확인한다.
4. 패키지 배치나 DTO/포트 경계가 애매할 때만 `docs/architecture-rule-eda.md`를 참고한다.

문서가 충돌하면 `AGENTS.md`가 이긴다.

## 범위 제한

다음은 도입하지 않는다.

- Outbox pattern
- DLQ, DLT, dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- `RestTemplate`, `WebClient`, OpenFeign 같은 직접 서비스 간 HTTP 호출

서비스 간 흐름은 기존 Kafka event/command/result 방식을 유지한다.

## 핵심 규칙

- Application service와 use case service에 domain policy value, compensation/idempotency key, event/command type string, service name, cross-service workflow identifier를 class constant로 두지 않는다.
- `private static final` 문자열로 magic string을 포장하지 않는다. finite concept이면 enum 또는 value object로 표현한다.
- `RENT_POINT`, `RETURN_POINT`, rental limit, rental period 같은 도메인 정책값은 domain model, domain enum, domain VO, 또는 domain policy object에 둔다.
- 보상 멱등성 키는 `RentalCompensationType` 같은 service-local typed enum/value object로 표현한다.
- 서비스 간 message protocol에 들어가는 reason/type은 필요할 때만 `common-events`의 shared enum으로 둔다.
- DB/Mongo에는 문자열로 저장해도 되지만, 문자열 변환은 persistence adapter에서만 수행한다.
- Runtime-configurable technical value, 예를 들어 Redis processing TTL은 `config`의 configuration properties로 이동한다.
- Domain model 내부의 아주 작은 정책 상수는 즉시 금지하지 않되, 정책 변경 가능성이 있거나 여러 곳에서 의미를 공유하면 domain policy 타입으로 분리한다.

## 분류 기준

먼저 상수의 성격을 분류한다.

| 성격 | 예시 | 권장 위치 |
|------|------|-----------|
| 도메인 정책값 | `RENT_POINT`, `RETURN_POINT`, `MAX_RENTAL_COUNT`, `RENTAL_DAYS` | `domain/model`의 policy enum/object 또는 domain behavior |
| 보상 멱등성 키 | `RENTAL_RENT_CANCEL`, `MEMBER_RENT_POINT_USE` | service-local compensation type enum/value object |
| Command reason | `RENT_COMPENSATION`, `RETURN_COMPENSATION` | `common-events` shared enum 또는 service-local enum |
| 메시지 타입 문자열 | `"ItemRented"`, `"EventResult"` | typed message enum 또는 adapter-local derivation |
| 서비스 식별자 | `SERVICE_NAME` | port에서 제거, 또는 adapter/config 책임으로 이동 |
| 기술 설정값 | `PROCESSING_TTL` | `config` configuration properties |
| 순수 구현 세부사항 | private regex, local format, short-lived helper | 필요한 경우 가까운 위치에 유지 가능 |

## 권장 적용 순서

### 1. 검색과 분류

먼저 전체 상수와 magic string 사용을 찾는다.

```powershell
rg "private static final|public static final|static final" -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
rg '"RENTAL_RENT_CANCEL"|"RENTAL_RETURN_CANCEL"|"MEMBER_RENT_POINT_USE"|"RENT_COMPENSATION"|"RETURN_COMPENSATION"|"ItemRented"|"ItemReturned"|"EventResult"|"PointUseCommand"' -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
```

검색 결과를 P0, P1, P2로 나눈다.

- P0: application service의 domain policy value, compensation/idempotency key, command reason magic string
- P1: application service의 service name, message type string
- P2: Kafka consumer TTL, domain model 내부 정책 상수

### 2. P0부터 한 slice로 적용

`RentalCardService`처럼 정책값과 보상 키가 섞인 클래스부터 처리한다.

권장 작업:

1. `RentalPointPolicy` 같은 domain policy enum/object를 추가한다.
2. `RentalCompensationType` 같은 typed compensation key를 추가한다.
3. `PointUseReason` 같은 command reason type을 추가한다.
4. `CompensationIdempotencyPort`가 `String` 대신 typed key를 받게 변경한다.
5. Persistence adapter에서만 `.name()` 또는 명시적 persistence value로 변환한다.
6. `PointUseCommand`와 service-local command의 reason field를 typed enum으로 바꾼다.
7. Application service의 class constants와 magic string reason을 삭제한다.
8. Producer/consumer/test fixture를 같은 slice에서 수정한다.

### 3. P1은 port 의미부터 정리

`SERVICE_NAME`과 message type string은 port가 정말 application service에서 받아야 하는지 먼저 판단한다.

- 서비스별 idempotency store가 분리되어 있으면 `serviceName` 파라미터 제거를 우선 검토한다.
- 감사 용도로 service name이 필요하면 adapter/config에서 공급한다.
- message type이 audit 구분이면 service-local enum을 쓴다.
- message type이 shared protocol이면 `common-events` enum으로 둔다.

### 4. P2는 기술값과 도메인 정책값을 분리

- Kafka consumer `PROCESSING_TTL`은 configuration properties로 이동한다.
- `MAX_RENTAL_COUNT`, `RENTAL_DAYS`는 domain model 내부에 남겨도 되는지 판단하고, 변경 가능성이 있으면 domain policy로 분리한다.

## 패키지 배치

기존 `AGENTS.md` target structure를 유지한다.

- Domain policy enum/object: 우선 `domain/model`
- Simple domain value object: `domain/vo`
- Application command/result: `application/dto`
- Compensation/message idempotency outbound port: `application/port/out`
- Persistence conversion: `adapter/out/persistence`
- Technical runtime property: `config`
- Shared message protocol enum: 실제 서비스 간 메시지 계약일 때만 `common-events/src/main/java/com/example/library/common/event`

새 `domain/policy` 패키지는 사용자가 명시적으로 구조 변경을 요청하지 않으면 만들지 않는다.

## 금지 패턴

다음 패턴을 제거하거나 새로 만들지 않는다.

- `RentalCardService` 같은 application service의 `private static final long RENT_POINT`
- `private static final String RENTAL_RENT_CANCEL`
- `private static final String SERVICE_NAME`
- `markCompensated(correlationId, "RENTAL_RENT_CANCEL")`
- `markProcessed("book-service", eventId, correlationId, "ItemRented")`
- 이유 또는 타입을 나타내는 string constant만 모아둔 `Constants` 클래스
- Domain model이나 domain VO가 persistence/string protocol 변환을 직접 수행

## 허용 패턴

다음은 허용한다.

- Persistence adapter에서 enum을 DB 저장 문자열로 변환
- Messaging adapter에서 domain event의 enum/value를 Kafka message field로 변환
- Config properties에서 TTL 같은 runtime technical value 주입
- 테스트 fixture에서 typed enum/value object 직접 사용
- 순수 구현 세부사항인 local constant를 가까운 private scope에 유지

## 리뷰 체크리스트

- Application service에 도메인 정책값 class constant가 없다.
- Application service에 보상/멱등성 키 문자열 class constant가 없다.
- Finite concept은 enum 또는 value object로 표현된다.
- DB/Mongo 저장 문자열 변환은 adapter에만 있다.
- `common-events`에 올린 enum은 실제 서비스 간 message protocol에 필요한 타입이다.
- 서비스 로컬 개념을 `common-events`로 올리지 않았다.
- Consumer TTL 같은 기술값은 `config` properties로 이동했다.
- Domain model 내부 정책 상수는 유지 또는 분리 이유가 분명하다.
- Outbox, DLQ/DLT, tracing, custom retry/backoff, SAGA orchestration을 새로 도입하지 않았다.

## 검증

공유 계약이나 여러 서비스를 건드리면 먼저 컴파일을 확인한다.

```powershell
.\gradlew.bat compileJava compileTestJava
```

P0 rental-service slice를 바꿨으면 관련 테스트를 우선 실행한다.

```powershell
.\gradlew.bat :common-events:test :rental-service:test --tests com.example.library.rental.domain.model.RentalCardTest --tests com.example.library.rental.application.dto.RentalSagaStateTest
```

P1 message idempotency slice를 여러 서비스에 적용했으면 관련 모듈 테스트를 실행한다.

```powershell
.\gradlew.bat :book-service:test :member-service:test :rental-service:test :bestbook-service:test
```

마지막에 잔여 상수와 magic string을 다시 검색하고, 허용된 것과 제거 대상이 남았는지 보고한다.

```powershell
rg "private static final|public static final|static final" -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
rg '"RENTAL_RENT_CANCEL"|"RENTAL_RETURN_CANCEL"|"MEMBER_RENT_POINT_USE"|"RENT_COMPENSATION"|"RETURN_COMPENSATION"|"ItemRented"|"ItemReturned"|"EventResult"|"PointUseCommand"' -n rental-service/src/main/java book-service/src/main/java member-service/src/main/java bestbook-service/src/main/java common-events/src/main/java
```

전체 테스트가 환경이나 DB 문제로 실패하면 기존 실패와 이번 리팩터링 실패를 구분해서 보고한다.
