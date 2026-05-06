---
name: apply-web-request-command-domain-vo-boundary
description: library-rental-eda에서 docs/web-request-command-domain-vo-boundary-plan.md의 Web Request DTO, application Command/Query, domain VO 경계 개선안을 적용하거나 리뷰할 때 사용한다. Trigger when refactoring inbound web or messaging adapters that create domain VO directly, replacing request methods like toIdName(), toItem(), toRentalMember(), or toDomainVo() with toCommand()/toQuery(), keeping application Command/Query as primitive/simple use-case input records, and creating service-local domain/vo objects inside application services or persistence mappers. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# Web Request, Command, Domain VO 경계 적용

## 목적

이 스킬은 `library-rental-eda`에서 inbound adapter DTO가 service domain VO를 직접 만들지 않도록 경계를 정리할 때 사용한다. Web/Kafka inbound adapter는 application Command/Query까지만 만들고, application service가 domain model 호출 직전에 domain VO를 생성하게 한다.

## 필수 읽기

코드를 변경하기 전에 다음 문서를 순서대로 읽는다.

1. `AGENTS.md`를 먼저 읽고 최우선 규칙으로 따른다.
2. `docs/web-request-command-domain-vo-boundary-plan.md`를 이 개선안의 source of truth로 삼는다.
3. 패키지 배치, DTO 분리, EDA naming이 애매할 때만 `docs/architecture-rule-eda.md`를 참고한다.

문서가 충돌하면 `AGENTS.md`가 이긴다. `AGENTS.md` 보완이 요청 범위에 포함되면 문서의 "AGENTS.md 보완 후보"를 반영하되 기존 프로젝트 예외 규칙을 유지한다.

## 핵심 규칙

- Web Request DTO는 HTTP 요청 계약이며 validation annotation, JSON alias, `toCommand()` 또는 `toQuery()`만 가진다.
- Kafka inbound consumer는 shared message를 application Command로 바꾸고 use case에 위임한다.
- Application Command/Query record는 use case 입력 계약이며 primitive/simple field를 우선 사용한다.
- Application Command/Query는 Web Request를 import하거나 `fromRequest()`를 제공하지 않는다.
- Application service는 Command/Query 값을 domain VO로 변환한 뒤 domain model을 호출한다.
- Domain VO는 service-local `domain/vo`에 두고 HTTP/Kafka/JPA/Spring annotation과 adapter 변환 메서드를 넣지 않는다.
- Persistence mapper는 Entity/Document와 domain model 복원을 담당하므로 domain VO를 생성해도 된다.
- Messaging outbound adapter는 local domain/application event의 VO 값을 Kafka message의 flat snapshot field로 풀어낸다.

## 금지 패턴

다음 패턴을 제거하거나 새로 만들지 않는다.

- Web Request DTO의 `toIdName()`, `toItem()`, `toRentalMember()`, `toRentalItem()`, `toDomainVo()`
- Web Request DTO가 `RentalMember`, `RentalItem` 같은 `domain/vo`를 import
- Application Command/Query record가 domain VO field를 기본 입력 형태로 사용
- Application Command/Query record의 `fromRequest()`
- Domain model이나 domain VO의 `toResponse()`, `toCommonEvent()`, `toJpaEntity()`
- `common-events` message field에 service domain VO 사용
- 다른 서비스의 `domain/vo` import

내부 전용 application method가 이미 domain VO를 받고 있고 adapter-facing 입력이 아니라면 무리하게 바꾸지 않는다. 우선 inbound adapter가 닿는 use case slice를 정리한다.

## 권장 흐름

Web inbound 흐름은 다음 모양을 따른다.

```text
adapter/in/web/dto Request
        -> toCommand() 또는 toQuery()
application/dto Command 또는 Query
        -> application service
domain/vo 생성
        -> domain/model 호출
```

Kafka inbound 흐름도 같은 기준을 따른다.

```text
common-events message
        -> adapter-local toCommand(message)
application/dto Command
        -> application service
domain/vo 생성
        -> domain/model 호출
```

Persistence mapper는 저장 representation과 domain representation 사이의 변환 경계이므로 다음 흐름을 유지한다.

```text
adapter/out/persistence Entity 또는 Document
        -> mapper
domain/vo 및 domain/model 복원
```

## 적용 절차

1. 현재 adapter DTO와 consumer의 domain VO 직접 생성을 검색한다.

   ```powershell
   rg "toIdName\(|toItem\(|toRentalMember\(|toRentalItem\(|toDomainVo\(|new RentalMember|new RentalItem"
   rg "adapter[\\/].*web.*import .*domain\\.vo|adapter[\\/].*messaging.*import .*domain\\.vo"
   ```

2. use case별 Command/Query record가 없거나 domain VO field를 기본 입력으로 쓰면 `application/dto`에 primitive/simple field 기반 record를 추가하거나 정리한다.
3. Web Request DTO는 `toCommand()` 또는 `toQuery()`만 공개하도록 변경한다.
4. Controller는 `request.toCommand()` 또는 `request.toQuery()`를 inbound port에 전달한다.
5. Inbound port는 adapter-facing use case에 대해 Command/Query를 받도록 정리한다.
6. Application service에서 Command/Query 값을 `new RentalMember(...)`, `new RentalItem(...)` 같은 domain VO로 변환한다.
7. Domain model API는 domain VO를 받는 기존 도메인 언어를 유지한다.
8. Response DTO는 application Result에서 생성하고, Result는 필요할 때 domain model에서 생성한다.
9. Kafka consumer를 건드리는 경우 consumer-local mapper나 private method에서 message -> Command 변환만 수행한다.
10. Persistence mapper의 domain VO 생성은 유지하되, mapper 밖 persistence entity 노출은 늘리지 않는다.

## 리뷰 체크리스트

- Web Request DTO가 domain VO를 import하지 않는다.
- Web Request DTO의 공개 변환 메서드는 `toCommand()` 또는 `toQuery()`다.
- Request DTO에 `toIdName()`, `toItem()`, `toRentalMember()` 같은 domain VO 반환 메서드가 없다.
- Application Command/Query가 adapter DTO를 import하지 않는다.
- Adapter-facing Command/Query는 primitive/simple use-case input field를 우선 사용한다.
- Application service가 domain model 호출 직전에 domain VO를 생성한다.
- Kafka inbound consumer는 message를 domain model에 직접 전달하지 않는다.
- Persistence mapper의 domain VO 생성은 representation 복원 경계 안에 남아 있다.
- `common-events`에는 service domain VO가 없다.

## 검증

작업 범위에 맞춰 targeted Gradle check를 우선 실행한다.

- rental-service web request 경계만 바꿨으면 `.\gradlew.bat :rental-service:test`를 우선 검토한다.
- 여러 모듈 또는 공유 계약을 건드렸으면 `.\gradlew.bat compileJava compileTestJava`를 실행한다.
- 실용적으로 가능하면 `.\gradlew.bat test`를 실행한다.
- 최소한 잔여 변환 검색을 실행하고 결과를 보고한다.

```powershell
rg "toIdName\(|toItem\(|toRentalMember\(|toRentalItem\(|toDomainVo\("
rg "adapter[\\/].*web.*import .*domain\\.vo|adapter[\\/].*messaging.*import .*domain\\.vo"
```
