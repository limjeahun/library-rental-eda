# Web Request, Application Command, Domain VO 경계 개선안

## 목적

이 문서는 Web Request DTO가 `domain/vo`를 직접 생성하지 않고, application Command까지 변환한 뒤 application service에서 domain VO를 생성하도록 경계를 정리하는 개선안이다.

기존 `common-events` 공통 VO 제거 작업의 연장선에 있다. `IDName`, `Item`을 `common-events`에서 제거했더라도 Web Request, application Command, domain VO의 책임이 다시 섞이면 같은 결합 문제가 서비스 내부에서 반복될 수 있다.

핵심 결론은 다음과 같다.

- Web Request DTO는 HTTP 요청 계약이다.
- Application Command는 use case 입력 계약이다.
- Domain VO는 도메인 모델을 호출하기 직전에 application service에서 생성한다.
- Domain VO는 domain model, application service, persistence mapper처럼 도메인 복원이나 도메인 호출이 필요한 곳에서 사용한다.
- Web Request DTO가 `new RentalMember(...)`, `new RentalItem(...)` 같은 domain VO 생성을 직접 담당하지 않도록 한다.

## 배경

현재 논의된 예시는 다음과 같다.

```java
package com.example.library.rental.adapter.in.web.request;

import com.example.library.rental.domain.vo.RentalMember;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
    @NotBlank @JsonAlias({"UserId", "userId"}) String userId,
    @NotBlank @JsonAlias({"UserNm", "userNm"}) String userNm
) {
    public RentalMember toIdName() {
        return new RentalMember(userId, userNm);
    }
}
```

이 코드는 Hexagonal Architecture의 의존 방향만 보면 동작 가능하다. adapter는 안쪽 계층인 domain에 의존할 수 있기 때문이다.

하지만 계층별 DTO 분리 기준을 더 엄격하게 적용하면 아쉬운 점이 있다.

- `UserRequest`는 HTTP JSON 계약인데 domain VO 생성까지 담당한다.
- `toIdName()`은 제거한 `IDName` 공통 VO의 이름 흔적을 남긴다.
- application Command를 건너뛰고 Web DTO가 domain 언어로 바로 진입한다.
- 이후 request DTO마다 `toRentalMember()`, `toRentalItem()` 같은 domain 변환 메서드가 늘어날 수 있다.

## 개선 방향

Web Request DTO는 application Command 또는 Query로만 변환한다.

```java
public CreateRentalCardCommand toCommand() {
    return new CreateRentalCardCommand(userId, userNm);
}
```

Application Command는 use case 입력값을 표현한다.

```java
package com.example.library.rental.application.dto;

public record CreateRentalCardCommand(
    String userId,
    String userNm
) {
}
```

Application service가 Command를 받아 domain VO를 생성하고 domain model을 호출한다.

```java
public RentalCardResult createRentalCard(CreateRentalCardCommand command) {
    RentalMember member = new RentalMember(command.userId(), command.userNm());
    RentalCard rentalCard = RentalCard.createRentalCard(member);
    return RentalCardResult.from(rentalCard);
}
```

권장 흐름은 다음과 같다.

```text
adapter/in/web/request UserRequest
        ↓ toCommand()
application/dto CreateRentalCardCommand
        ↓ application service
domain/vo RentalMember
        ↓
domain/model RentalCard
```

## 책임 분리 기준

| 계층 | 책임 | 권장 타입 | 피해야 할 것 |
|------|------|-----------|--------------|
| `adapter/in/web/request` | HTTP 요청 계약, validation annotation, request field alias | Request record, `toCommand()` | domain VO 직접 반환, `toIdName()`, `toItem()` |
| `application/dto` | use case 입력/출력 계약 | Command, Query, Result record | Web Request import, `fromRequest()` |
| `application/service` | use case orchestration, domain 호출 준비 | Command -> domain VO 생성 | Web Response 생성, Kafka message 생성 |
| `domain/vo` | 도메인 의미와 불변 조건 | `RentalMember`, `RentalItem`, `MemberIdentity` | HTTP/Kafka/JPA annotation |
| `domain/model` | 비즈니스 상태 전이 | aggregate, child model, domain VO | Request/Response/Kafka message 참조 |
| `adapter/out/persistence` | Entity/Document와 domain model 변환 | mapper에서 `new RentalMember(...)` | domain model에 JPA annotation 전파 |
| `adapter/in/messaging` | Kafka message 수신 후 use case 위임 | message -> Command | message를 domain model에 직접 전달 |
| `adapter/out/messaging` | local event/result를 Kafka message로 변환 | domain VO -> flat message field | domain model에 `toCommonEvent()` 추가 |

## 허용되는 Domain VO 생성 위치

Domain VO 생성은 금지되는 것이 아니라 위치가 중요하다.

허용한다.

- `application/service`에서 Command 값을 domain model 호출 전에 domain VO로 변환
- `domain/model` 내부에서 다른 domain VO 또는 child model 생성
- `adapter/out/persistence` mapper에서 Entity/Document를 domain model로 복원
- `adapter/out/messaging`에서 domain event의 VO 값을 Kafka message의 flat field로 풀어냄
- 테스트 fixture에서 domain model을 직접 테스트하기 위해 domain VO 생성

피한다.

- Web Request DTO가 domain VO를 직접 반환
- Application Command record가 `fromRequest()`를 제공
- Domain VO가 Web Response, Kafka message, JPA Entity로 변환하는 메서드를 가짐
- `common-events` message가 service domain VO를 필드로 가짐
- 다른 서비스가 특정 서비스의 `domain/vo`를 import

## Before / After

### UserRequest

변경 전:

```java
public record UserRequest(
    String userId,
    String userNm
) {
    public RentalMember toIdName() {
        return new RentalMember(userId, userNm);
    }
}
```

변경 후:

```java
public record UserRequest(
    String userId,
    String userNm
) {
    public CreateRentalCardCommand toCommand() {
        return new CreateRentalCardCommand(userId, userNm);
    }
}
```

### UserItemRequest

변경 전:

```java
public record UserItemRequest(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
    public RentalMember toIdName() {
        return new RentalMember(userId, userNm);
    }

    public RentalItem toItem() {
        return new RentalItem(itemNo, itemTitle);
    }
}
```

변경 후:

```java
public record UserItemRequest(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
    public RentItemCommand toCommand() {
        return new RentItemCommand(userId, userNm, itemNo, itemTitle);
    }
}
```

### Application Service

변경 후 application service에서 domain VO를 생성한다.

```java
public RentalCardResult rentItem(RentItemCommand command) {
    RentalMember member = new RentalMember(command.userId(), command.userNm());
    RentalItem item = new RentalItem(command.itemNo(), command.itemTitle());
    RentalCard rentalCard = rentalCardRepository.loadRentalCard(member.id())
        .orElseGet(() -> RentalCard.createRentalCard(member));

    rentalCard.rentItem(item);
    return RentalCardResult.from(rentalCardRepository.save(rentalCard));
}
```

## Command 설계 기준

Application Command는 use case의 입력 언어다.

권장한다.

```java
public record RentItemCommand(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
}
```

다음 형태는 이번 개선안에서는 피한다.

```java
public record RentItemCommand(
    RentalMember member,
    RentalItem item
) {
}
```

두 번째 형태도 컴파일이나 의존 방향만 보면 가능하지만, Web/Kafka inbound adapter가 application Command를 만들기 위해 domain VO를 먼저 알아야 한다. 그러면 inbound adapter와 domain VO의 결합이 넓어진다.

이 개선안에서는 application Command가 primitive 또는 simple field를 보관하고, application service가 도메인 진입점에서 domain VO를 생성하는 쪽을 기본 규칙으로 삼는다.

## Kafka Inbound에도 같은 기준 적용

Kafka consumer도 같은 원칙을 따른다.

```text
common-events message
        ↓ adapter/in/messaging private toCommand(message)
application Command
        ↓ application service
domain VO 생성
        ↓ domain model 호출
```

예시:

```java
private RentItemCommand toCommand(ItemRented event) {
    return new RentItemCommand(
        event.memberId(),
        event.memberName(),
        event.itemNo(),
        event.itemTitle()
    );
}
```

consumer에서 `new RentalMember(...)`와 `new RentalItem(...)`을 직접 만들지 않고, application service로 Command를 넘긴다.

## Persistence Mapper는 예외가 아니라 올바른 위치

Persistence mapper는 저장 모델을 domain model로 복원하는 adapter다. 따라서 mapper에서 domain VO를 생성하는 것은 자연스럽다.

```java
private RentalCard toDomain(RentalCardJpaEntity entity) {
    RentalMember member = new RentalMember(entity.getMemberId(), entity.getMemberName());
    ...
}
```

이 경우는 Web Request DTO와 다르다. mapper의 목적 자체가 persistence representation과 domain representation 사이를 변환하는 것이기 때문이다.

## AGENTS.md 보완 후보

현재 `AGENTS.md`는 DTO와 변환 위치의 큰 방향을 이미 포함한다. 다만 application Command가 domain VO를 가질 수 있는지에 대한 해석 여지가 있다.

이 개선안을 프로젝트 규칙으로 고정하려면 `DTO and Conversion Rules`에 다음 문구를 추가할 수 있다.

```text
- Web Request DTOs should convert to application Command or Query using primitive or simple request fields, not service domain value objects.
- Application services should create service domain value objects from application Command or Query values immediately before invoking domain models.
- Application Command and Query records should prefer primitive or simple use-case input fields over domain value object fields unless the use case is purely internal and has no adapter-facing input.
- Inbound web and messaging adapters should not expose methods such as `toIdName()`, `toItem()`, or `toDomainVo()` on request/message DTOs.
```

## 적용 대상

우선 적용 대상은 rental-service의 inbound web request다.

- `adapter/in/web/request/UserRequest`
- `adapter/in/web/request/UserItemRequest`
- `adapter/in/web/request/ClearOverdueRequest`
- `adapter/in/web/RentalCardController`
- `application/dto`의 use case Command 추가 또는 정리
- `application/port/in`의 메서드 시그니처 정리
- `application/service/RentalCardService`

이후 같은 기준을 member-service와 Kafka inbound command에도 확장할 수 있다.

## 적용 순서

1. Web Request DTO의 `toIdName()`, `toItem()` 메서드를 찾는다.
2. 각 use case별 application Command record를 `application/dto`에 추가한다.
3. Web Request DTO는 `toCommand()`만 제공하도록 변경한다.
4. Controller는 `request.toCommand()`를 use case에 전달한다.
5. Inbound port는 primitive/simple field 기반 Command를 받도록 변경한다.
6. Application service에서 Command 값을 domain VO로 변환한다.
7. Domain model은 기존처럼 domain VO를 받는다.
8. Response DTO는 application Result에서 생성하도록 유지한다.
9. `rg "toIdName\\(|toItem\\("`로 inbound web request의 잔여 변환을 확인한다.
10. `.\gradlew.bat compileJava compileTestJava`와 관련 module test를 실행한다.

## 리뷰 체크리스트

- Web Request DTO가 domain VO를 import하지 않는다.
- Web Request DTO의 공개 변환 메서드는 `toCommand()` 또는 `toQuery()`다.
- `toIdName()`, `toItem()`, `toRentalMember()` 같은 domain VO 반환 메서드가 request DTO에 없다.
- Application Command가 Web Request를 import하지 않는다.
- Application Command는 primitive/simple use case input field를 우선 사용한다.
- Application service가 domain model 호출 직전에 domain VO를 생성한다.
- Persistence mapper의 domain VO 생성은 유지된다.
- Kafka outbound adapter는 domain event의 VO를 Kafka message flat field로 변환한다.
- `common-events`에는 service domain VO가 없다.

## 기대 효과

- Web API 계약과 domain VO가 더 명확히 분리된다.
- `IDName` 제거 후에도 `toIdName()` 같은 과거 공통 VO 흔적이 남지 않는다.
- application Command가 use case 입력 계약으로 선명해진다.
- domain VO 생성 위치가 application service와 persistence mapper 중심으로 정리된다.
- Web/Kafka inbound adapter가 domain VO 구조 변경에 덜 민감해진다.

## 주의점

- 이 변경은 메서드 시그니처가 넓게 바뀔 수 있으므로 controller, inbound port, application service, tests를 같은 slice에서 함께 수정해야 한다.
- Command field 이름은 API 이름을 그대로 복사하기보다 use case 언어에 맞게 정리한다. 예를 들어 외부 JSON alias는 `UserNm`일 수 있어도 Command에서는 `userName` 또는 프로젝트 기존 명명인 `userNm` 중 하나로 일관되게 선택한다.
- 이미 domain VO를 받는 내부 전용 application method까지 무리하게 바꿀 필요는 없다. 우선 adapter-facing inbound use case부터 정리한다.
