# common-events 공통 VO 제거 및 서비스 로컬 VO 전환 변경안

## 목적

현재 프로젝트는 `common-events/src/main/java/com/example/library/common/vo`에 `IDName`, `Item`을 두고 여러 서비스가 함께 사용한다.

```text
common-events
└── src/main/java/com/example/library/common
    ├── event
    └── vo
        ├── IDName.java
        └── Item.java
```

이 문서는 외부 문서를 근거로 삼지 않는다. 현재 코드에서 `common-events/common/vo`가 서비스 간 메시지 계약과 서비스 내부 도메인 VO 역할을 동시에 맡고 있기 때문에, 이 결합을 유지할지 아니면 DDD/Hexagonal 원칙에 맞춰 서비스 로컬 VO와 메시지 계약을 분리할지 판단하기 위한 변경안을 정리한다.

이 변경안의 핵심은 다음과 같다.

- `common-events`는 Kafka event/command/result 계약만 보유한다.
- `IDName`, `Item` 같은 도메인 의미의 값 객체는 각 서비스 내부 언어에 맞게 둔다.
- 공유 메시지에는 서비스 내부 VO를 중첩하지 않고 필요한 snapshot field만 둔다.
- 계층별 DTO 분리와 변환 메서드 소유 규칙을 함께 지킨다.
- 이 방향을 실제로 채택하려면 `AGENTS.md`의 `common value contracts` 규칙도 함께 조정한다.

## 현재 구조의 문제

현재 `IDName`, `Item`은 두 역할을 동시에 가진다.

- Kafka 메시지 payload에서 반복되는 공유 값 조각
- rental/member/bestbook 도메인과 application use case에서 직접 쓰는 값 객체

이 구조는 구현은 간단하지만 다음 문제가 있다.

- `common-events`의 메시지 계약 변경이 서비스 내부 도메인 모델에 영향을 준다.
- 서비스별 ubiquitous language가 `IDName`, `Item`이라는 같은 이름에 묶인다.
- `rental-service` domain code가 `common-events`의 `common.vo`에 의존한다.
- `common-events`가 이벤트 계약 모듈을 넘어 사실상 공유 도메인 모델 모듈처럼 사용된다.

## 목표 구조

`common-events`는 서비스 간 Kafka 메시지 계약만 보유한다.

```text
common-events
└── src/main/java/com/example/library/common
    └── event
        ├── ItemRented.java
        ├── ItemReturned.java
        ├── OverdueCleared.java
        ├── EventResult.java
        ├── PointUseCommand.java
        ├── Participant.java
        └── SagaStep.java
```

`common-events/common/vo` 패키지는 제거한다.

도메인 의미와 불변 조건을 가진 값 객체는 각 서비스의 `domain/vo`에 둔다. `application/dto`는 use case의 입력/출력인 Command, Query, Result를 위한 곳이며 도메인 VO를 대신 보관하는 위치가 아니다.

예상 예시:

```text
rental-service
├── domain/vo
│   ├── RentalMember.java
│   └── RentalItemInfo.java
├── application/dto
│   ├── RentItemCommand.java
│   ├── ReturnItemCommand.java
│   └── RentalCardResult.java
└── adapter/in/web/dto
    ├── RentItemRequest.java
    └── RentalCardResponse.java

member-service
├── domain/vo
│   └── MemberIdentity.java
└── application/dto
    ├── AddMemberCommand.java
    └── ChangePointCommand.java

bestbook-service
├── domain/vo
│   └── BestBookItem.java
└── application/dto
    └── RecordBestBookRentCommand.java
```

이름은 최종 구현 시 서비스의 언어에 맞춰 조정한다. 단순히 `IDName`, `Item`을 각 서비스에 복제할 수도 있지만, MSA/DDD 관점에서는 `RentalMember`, `RentalItemInfo`, `MemberIdentity`, `BestBookItem`처럼 서비스별 의미가 드러나는 이름이 더 안전하다.

## 계층별 DTO/VO 분리 기준

이 변경은 `common.vo` 패키지를 없애는 것만으로 끝나면 안 된다. 각 계층의 DTO와 VO 책임을 함께 분리해야 한다.

| 위치 | 역할 | 허용 | 금지 |
|------|------|------|------|
| `domain/vo` | 서비스 도메인 값 객체 | 서비스 언어, 불변 조건, 간단한 검증 | Spring/JPA/Kafka 의존, `common-events` 의존 |
| `domain/model` | Aggregate와 상태 전이에 참여하는 child model | 비즈니스 상태 변경, 도메인 규칙 | API Response 생성, Kafka message 생성 |
| `application/dto` | Use case 입력/출력 | Command, Query, Result record | Web/Kafka/JPA 어노테이션, adapter DTO 참조 |
| `adapter/in/web/dto` | HTTP API 계약 | Request, Response record | Domain entity 직접 반환, application service 안에서 생성 |
| `adapter/in/messaging/consumer` | Kafka inbound adapter | shared message 수신, 최소 검증, application command 변환 | 비즈니스 판단, shared message를 domain에 직접 전달 |
| `adapter/out/messaging` | Kafka outbound adapter | local domain/application event를 shared message로 변환 | domain model 안에 `toCommonEvent()` 추가 |
| `adapter/out/persistence` | DB 저장 계약 | Entity/Document, repository, mapper | domain model에 persistence annotation 전파 |
| `common-events` | 서비스 간 Kafka 계약 | event/command/result/protocol enum record | 서비스 도메인 VO, 서비스 application DTO |

중요한 기준은 다음과 같다.

- `common-events`의 record는 integration message이지 application DTO가 아니다.
- `application/dto`의 Command/Query/Result는 특정 use case의 언어를 담는다.
- `domain/vo`는 서비스 내부 모델의 일부이며 외부 메시지 스키마 변경 이유로 바뀌면 안 된다.
- Web Request/Response DTO와 Kafka message record는 서로 재사용하지 않는다.
- Persistence Entity/Document와 Domain Model 사이의 변환은 persistence mapper가 담당한다.

## 메시지 계약 변경 방향

공유 메시지 record는 더 이상 `IDName`, `Item`을 필드로 갖지 않는다. 대신 메시지 payload에 필요한 snapshot 값을 primitive 필드로 직접 둔다.

예시 변경 전:

```java
public record ItemRented(
    String eventId,
    String correlationId,
    Instant occurredAt,
    IDName idName,
    Item item,
    long point
) {
}
```

예시 변경 후:

```java
public record ItemRented(
    String eventId,
    String correlationId,
    Instant occurredAt,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point
) {
}
```

같은 방식으로 다음 메시지를 정리한다.

- `ItemRented`
- `ItemReturned`
- `OverdueCleared`
- `ItemRentCanceled`
- `ItemReturnCanceled`
- `OverdueClearCanceled`
- `PointUseCommand`
- `EventResult`

`OverdueCleared`, `OverdueClearCanceled`, `PointUseCommand`처럼 도서 정보가 필요 없는 메시지는 `memberId`, `memberName`, `point`만 유지한다.

## 변환 책임과 메서드 규칙

서비스 내부에서는 local VO와 application DTO를 사용하고, Kafka adapter에서만 `common-events` message record로 변환한다.

예상 흐름:

```text
adapter/in/web/dto Request
        ↓ request.toCommand()
application/dto Command
        ↓ application service
domain model / domain VO
        ↓ domain result
application/dto Result.from(domain)
        ↓ response mapping
adapter/in/web/dto Response.from(result)
```

Kafka inbound 흐름:

```text
common-events message record
        ↓ adapter/in/messaging mapper or private method
application/dto Command
        ↓ application service
domain model / domain VO
```

Kafka outbound 흐름:

```text
domain/application local event or result
        ↓
adapter/out/messaging mapper
        ↓
common-events Kafka message primitive fields
```

즉, `common-events` 메시지는 서비스 내부 모델이 아니라 외부 통합 계약이다.

변환 메서드 소유 규칙은 다음과 같이 잡는다.

| 변환 방향 | 위치 | 메서드 규칙 |
|----------|------|-------------|
| Web Request -> Application Command | `adapter/in/web/dto` Request | `toCommand()` |
| Application Result -> Web Response | `adapter/in/web/dto` Response | `from(result)` |
| Domain -> Application Result | `application/dto` Result | `from(domain)` |
| Persistence Entity/Document -> Domain | `adapter/out/persistence` mapper | `toDomain(entity)` |
| Domain -> Persistence Entity/Document | `adapter/out/persistence` mapper | `toEntity(domain)` 또는 현재 코드의 mapper 명명 유지 |
| Kafka Message -> Application Command | `adapter/in/messaging` mapper/private method | `toCommand(message)` 형태는 adapter 내부에만 둔다 |
| Domain/Application Event -> Kafka Message | `adapter/out/messaging` mapper/private method | `toMessage(localEvent)` 또는 `from(localEvent)` |

금지할 변환은 다음과 같다.

- `common-events` record에 `toRentalCommand()`, `toMemberCommand()` 같은 서비스별 변환 메서드를 두지 않는다.
- domain model이나 domain VO에 `toResponse()`, `toCommonEvent()`, `toJpaEntity()`를 두지 않는다.
- application Command에 `fromRequest()`를 두지 않는다. 상위 adapter DTO를 application 계층이 알게 되기 때문이다.
- application service가 Web Response나 Kafka message record를 직접 조립하지 않는다. 조립은 adapter가 담당한다.

## 서비스별 변경 범위

### common-events

- `common/vo/IDName.java` 삭제
- `common/vo/Item.java` 삭제
- event record 필드를 primitive snapshot으로 변경
- `EventResult.success(...)`, `EventResult.failure(...)` 팩토리 시그니처 변경
- `common-events` record에 서비스별 `toCommand()`나 `fromDomain()` 변환을 넣지 않음
- common-events 테스트도 primitive 필드 기준으로 변경

### rental-service

- `domain/vo`에 rental 도메인용 회원/도서 값 객체 추가
- `RentalCard`, `RentItem`, `RentalCardEvents`가 `common.vo`를 import하지 않도록 변경
- inbound web request는 `adapter/in/web/dto`의 `toCommand()`로 application command로 변환
- application port의 파라미터에서 `common.vo.IDName`, `common.vo.Item` 제거
- `RentalKafkaEventProducer` 또는 adapter-local mapper에서 local event/result를 `common-events` primitive message로 변환
- `RentalEventConsumer`는 primitive message를 application command로 변환한 뒤 use case에 위임
- `RentalSagaState`도 common VO 대신 local domain/application 타입 또는 primitive snapshot을 보관

### member-service

- `domain/vo/IDName` 또는 더 명확한 `MemberIdentity` 추가
- `Member`, `ChangePointCommand`, `AddMemberCommand`, repository mapper가 local VO를 사용하도록 변경
- Kafka consumer는 `ItemRented.memberId/memberName` 등을 local command로 변환
- `EventResult` 발행 시 adapter/out/messaging에서 local result를 primitive message field로 변환

### book-service

- book 도메인에는 이미 도서 aggregate가 있으므로 `Item` 공유 VO를 내부 모델로 가져오지 않는다.
- Kafka consumer는 `itemNo`, `itemTitle` 중 도서 상태 변경에 필요한 `itemNo`만 use case로 전달한다.
- Result Event 발행 시 adapter/out/messaging에서 메시지에 필요한 snapshot field를 primitive로 채운다.

### bestbook-service

- `BestBook.registerBestBook(Item item)` 같은 common VO 의존 메서드를 제거하거나 local `BestBookItem`으로 변경한다.
- Kafka consumer는 `itemNo`, `itemTitle`을 `RecordBestBookRentCommand`로 전달한다.
- `ItemRentCanceled`도 primitive field 기준으로 처리한다.

## 적용 순서

1. `AGENTS.md`에서 `common value contracts`와 `common-events/common/vo` 유지 문구를 먼저 조정한다.
2. 각 서비스에 필요한 `domain/vo`와 `application/dto` Command/Result를 계층별로 추가한다.
3. Web Request/Response, Kafka inbound/outbound, persistence mapper의 변환 책임을 정리한다.
4. 서비스 domain/application 코드에서 `common.vo` import를 제거한다.
5. `common-events` 메시지 record를 primitive snapshot 필드로 바꾼다.
6. 모든 producer adapter에서 local event/result를 primitive message로 변환한다.
7. 모든 consumer adapter에서 primitive message를 application command로 변환한다.
8. 테스트의 `new IDName(...)`, `new Item(...)` 사용을 서비스 로컬 타입 또는 primitive field로 변경한다.
9. `common-events/src/main/java/com/example/library/common/vo`를 삭제한다.
10. `rg "com.example.library.common.vo"`로 잔여 의존이 없는지 확인한다.
11. `./gradlew.bat compileJava compileTestJava`를 먼저 확인하고, 가능한 범위에서 `./gradlew.bat test`를 수행한다.

## AGENTS.md와의 관계

이 변경안은 현재 `AGENTS.md`와 일부 충돌한다. 코드 변경 전에 `AGENTS.md`를 먼저 정리해야 한다.

### 현재 충돌 지점

현재 `AGENTS.md`에는 다음 규칙이 있다.

```text
Shared event, command, result, and common value contracts belong in common-events when they are used by more than one service.
```

이 문구는 `IDName`, `Item`처럼 여러 서비스가 함께 쓰는 값 객체를 `common-events/common/vo`에 둘 수 있다는 해석을 허용한다. 반면 이 변경안은 `common-events`에서 도메인 VO를 제거하려는 방향이다.

또한 현재 `AGENTS.md`에는 다음 구조도 명시되어 있다.

```text
For common-events, keep shared contracts under:

com.example.library.common/
├── event/
└── vo/
```

이 구조 역시 `common-events/common/vo` 유지 방향이므로 함께 수정해야 한다.

### AGENTS.md 변경 목표

`AGENTS.md` 변경 목표는 다음과 같다.

- `common-events`는 공유 메시지 계약 모듈로 유지한다.
- 공유 프로토콜 enum, event, command, result는 `common-events`에 둔다.
- 서비스 도메인 VO는 `common-events`에 두지 않는다.
- 메시지에 필요한 식별자/표시명/도서 제목 등은 message snapshot field로 둔다.
- 메시지 전용 value type을 허용하더라도 service domain/application에서 재사용하지 않는다는 제한을 둔다.
- 계층별 DTO와 VO의 책임을 분리하고, 변환 메서드 소유 위치를 명시한다.

### 권장 수정안

아래는 `AGENTS.md`에 반영할 권장 문구이다.

#### Service Boundary Rules 수정

변경 전:

```text
Shared event, command, result, and common value contracts belong in `common-events` when they are used by more than one service.
```

변경 후:

```text
Shared event, command, result, and protocol enum contracts belong in `common-events` when they are used by more than one service.
Do not place service domain value objects in `common-events`.
When a Kafka message needs member, item, or other cross-service data, model it as immutable snapshot fields in the message contract, such as `memberId`, `memberName`, `itemNo`, and `itemTitle`.
```

#### Architecture Principles 수정

변경 전:

```text
Prefer Java `record` for DTOs, commands, events, and simple immutable message payloads when compatible with the existing code.
Keep simple immutable domain value objects under `domain/vo` and prefer Java `record` for them.
Shared message/protocol enums used across services belong in `common-events`, and adapter-only or persistence-only enums should stay in the relevant adapter package.
```

변경 후:

```text
Prefer Java `record` for DTOs, commands, events, and simple immutable message payloads when compatible with the existing code.
Keep simple immutable domain value objects under each service's `domain/vo` and prefer Java `record` for them.
Application DTOs under `application/dto` are use case Command, Query, and Result records; they are not a replacement location for domain value objects.
Do not reuse `common-events` message payload types as service domain value objects or application DTOs.
Shared message/protocol enums used across services belong in `common-events`, and adapter-only or persistence-only enums should stay in the relevant adapter package.
```

#### common-events Target Package Structure 수정

변경 전:

```text
For `common-events`, keep shared contracts under:

com.example.library.common/
├── event/
└── vo/
```

변경 후:

```text
For `common-events`, keep shared Kafka contracts under:

com.example.library.common/
└── event/

Do not create `common.vo` for service domain value objects.
If a message-only nested value type is truly necessary, place it under a message-specific package or name it explicitly as a message payload type, and do not use it from service domain models.
```

#### EDA Rules 수정

변경 전:

```text
Shared event, command, result, and common value contracts in `common-events` should be Java records and should be accessed with record accessors.
```

변경 후:

```text
Shared event, command, and result contracts in `common-events` should be Java records and should be accessed with record accessors.
Prefer primitive or simple snapshot fields in shared Kafka message records over shared domain VO fields.
Service-local domain/application code should convert to and from shared Kafka message records at adapter boundaries.
```

#### DTO and Conversion Rules 추가

새 규칙을 추가한다.

```text
## DTO and Conversion Rules

- Web request and response DTOs belong in `adapter/in/web/dto`.
- Application Command, Query, and Result records belong in `application/dto`.
- Domain value objects belong in each service's `domain/vo`, not in `application/dto`.
- Web Request DTOs may convert to application Command or Query with `toCommand()` or `toQuery()`.
- Web Response DTOs may convert from application Result with `from(result)`.
- Application Result records may convert from domain models with `from(domain)` when useful.
- Persistence Entity/Document and domain model conversion belongs in `adapter/out/persistence` mapper classes.
- Shared Kafka message records from `common-events` must not define service-specific conversion methods such as `toRentalCommand()` or `toMemberCommand()`.
- Kafka consumers or adapter-local messaging mappers convert shared Kafka messages to application commands.
- Kafka producers or adapter-local messaging mappers convert local domain/application events or results to shared Kafka messages.
- Domain models and domain value objects must not define `toResponse()`, `toCommonEvent()`, or `toJpaEntity()`.
- Application Command records must not define `fromRequest()` because that would make application DTOs depend on adapter DTOs.
```

### AGENTS.md 변경 후 코드 적용 원칙

`AGENTS.md`가 위 방향으로 수정되면 코드 변경은 다음 원칙을 따른다.

- `common-events/src/main/java/com/example/library/common/vo`는 제거 대상이다.
- `common-events`의 event record는 `IDName`, `Item`을 import하지 않는다.
- `common-events` message record는 adapter 밖으로 넓게 퍼지지 않게 하고, 특히 domain model과 application DTO로 재사용하지 않는다.
- rental/member/bestbook domain model은 각자 local VO를 사용한다.
- web request DTO, persistence mapper, Kafka consumer/producer adapter가 local VO와 message snapshot field 사이의 변환을 담당한다.
- application service는 가능하면 service-local DTO/VO만 사용한다.
- adapter 밖으로 `common-events` payload type이 새어나가는 경우는 inbound/outbound port의 성격에 따라 별도로 판단하되, domain model에는 들어가지 않게 한다.

### 적용 전 결정해야 할 사항

실제 구현 전에 아래를 먼저 결정한다.

- 메시지 record에 flat field만 둘지, message-only nested record를 둘지
- 각 서비스 local VO 이름을 단순한 공통 명칭인 `IDName`, `Item`으로 둘지, 서비스 언어를 살려 `RentalMember`, `RentalItemInfo`, `MemberIdentity`처럼 둘지
- 기존 Kafka 메시지와의 호환성을 버리고 한 번에 바꿀지, v2 메시지/토픽을 둘지
- `EventResult`에 `itemNo/itemTitle`이 항상 필요한지, eventType/step에 따라 nullable field를 허용할지

### 권장 결정

현재 프로젝트 규모에서는 다음 결정을 권장한다.

- 메시지 record는 flat snapshot field를 사용한다.
- 서비스 local VO는 서비스 의미가 드러나는 이름을 사용한다.
- 기존 개발 중인 메시지 계약은 한 번에 변경한다.
- `EventResult`는 nullable `itemNo`, `itemTitle`을 허용하되, 생성 팩토리에서 흐름별 필수값을 검증한다.

## 기대 효과

- 서비스 도메인 모델이 `common-events`에 덜 결합된다.
- `IDName`, `Item`의 의미가 서비스별 언어로 분리된다.
- Kafka 메시지 계약과 내부 도메인 모델의 변경 이유가 분리된다.
- `common-events`가 공유 도메인 모델 모듈처럼 커지는 것을 막는다.

## 주의점

- 이 변경은 단순 패키지 이동이 아니라 공유 메시지 계약 변경이다.
- 모든 producer와 consumer를 같은 slice에서 함께 수정해야 한다.
- 이미 발행된 Kafka 메시지와의 호환성을 고려해야 한다.
- 운영 중인 환경이라면 메시지 버전 필드 또는 신규 토픽/신규 이벤트 타입 전환 전략이 필요할 수 있다.
- 현재 개발 환경에서는 `rental-service`의 MariaDB 의존 persistence test가 실패하는 상태이므로, 전체 `test` 결과와 코드 변경 실패를 구분해야 한다.
