---
name: apply-common-events-vo-refactoring
description: library-rental-eda에서 common-events/common/vo의 IDName, Item 같은 공유 도메인 VO를 제거하거나 리뷰하고, 서비스 로컬 domain/vo, 계층별 DTO 분리, Kafka 메시지 snapshot field, adapter 경계 변환 규칙을 적용할 때 사용한다. docs/common-events-vo-refactoring-plan.md 적용, common-events value contract 변경, AGENTS.md의 common-events 규칙 변경, Web DTO/application Command/Result/domain VO/persistence mapper/Kafka message 변환 분리 작업에 사용한다. 관련 없는 UI 작업, 직접 서비스 HTTP 호출, Outbox, DLQ/DLT, 분산 추적, custom Kafka retry/backoff, SAGA orchestration에는 사용하지 않는다.
---

# common-events VO 리팩터링 적용

## 목적

이 스킬은 `library-rental-eda`에서 `common-events/common/vo` 제거 변경안을 적용하거나 리뷰할 때 사용한다. 목표는 `common-events`를 Kafka 통합 메시지 계약 모듈로 유지하고, 서비스 도메인 값 객체는 각 서비스 경계 안으로 분리하는 것이다.

## 필수 확인 문서

코드를 변경하기 전에 다음 문서를 순서대로 읽는다.

1. `AGENTS.md`를 먼저 읽고 최우선 규칙으로 따른다.
2. `docs/common-events-vo-refactoring-plan.md`를 이 리팩터링의 source of truth로 삼는다.
3. 패키지 배치, DTO 분리, 변환 메서드 소유 위치가 애매할 때만 `docs/architecture-rule-eda.md`를 참고한다.

문서가 충돌하면 `AGENTS.md`가 이긴다. 변경안 적용을 위해 `AGENTS.md` 수정이 필요하면, `AGENTS.md`를 먼저 수정하거나 첫 번째 작업 slice로 포함한다.

## 작업 범위

이 스킬의 범위는 다음과 같다.

- `common-events/src/main/java/com/example/library/common/vo` 제거
- 공유 `IDName`, `Item` 사용을 서비스 로컬 `domain/vo` 타입으로 대체
- `common-events`에는 공유 Kafka event, command, result, protocol enum record만 유지
- 서비스 간 메시지에 필요한 값은 `memberId`, `memberName`, `itemNo`, `itemTitle` 같은 immutable snapshot field로 모델링
- Web DTO, application Command/Query/Result, domain VO/model, persistence entity/document, Kafka message record의 계층별 분리 유지
- 변환 로직을 올바른 adapter 또는 DTO 경계로 이동

다음은 도입하지 않는다.

- Outbox pattern
- DLQ, DLT, dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- `RestTemplate`, `WebClient`, OpenFeign 같은 직접 서비스 간 HTTP 호출

## 핵심 규칙

- `common-events` record는 integration message이며 domain model이나 application DTO가 아니다.
- 서비스 도메인 VO를 `common-events`에 두지 않는다.
- Web Request/Response DTO를 Kafka message로 재사용하지 않는다.
- Kafka message를 application Command/Query/Result로 재사용하지 않는다.
- 도메인 값 객체는 각 서비스의 `domain/vo`에 둔다.
- application use case 입력/출력은 `application/dto`에 둔다.
- HTTP Request/Response record는 `adapter/in/web/dto`에 둔다.
- Kafka consumer는 얇게 유지한다. 역직렬화, 최소 검증, application command 변환, use case 위임만 수행한다.
- Kafka producer는 `adapter/out/messaging`에 두고, local domain/application event 또는 result를 shared Kafka message로 변환한다.
- persistence 변환은 `adapter/out/persistence` mapper class가 담당한다.

## 변환 메서드 소유 규칙

다음 변환 위치와 메서드 이름을 따른다.

| 변환 방향 | 소유 위치 | 메서드 |
|----------|----------|--------|
| Web Request -> Application Command/Query | `adapter/in/web/dto` request | `toCommand()` 또는 `toQuery()` |
| Application Result -> Web Response | `adapter/in/web/dto` response | `from(result)` |
| Domain -> Application Result | `application/dto` result | 필요하면 `from(domain)` |
| Persistence Entity/Document -> Domain | `adapter/out/persistence` mapper | `toDomain(entity)` |
| Domain -> Persistence Entity/Document | `adapter/out/persistence` mapper | `toEntity(domain)` 또는 기존 mapper 명명 유지 |
| Kafka Message -> Application Command | `adapter/in/messaging` mapper 또는 private method | adapter 내부 `toCommand(message)` |
| Domain/Application Event -> Kafka Message | `adapter/out/messaging` mapper 또는 private method | adapter 내부 `toMessage(localEvent)` 또는 `from(localEvent)` |

다음 변환은 만들지 않는다.

- `common-events` record에 `toRentalCommand()`, `toMemberCommand()` 같은 서비스별 변환 메서드 추가
- domain model 또는 domain VO에 `toResponse()`, `toCommonEvent()`, `toJpaEntity()` 추가
- application Command에 `fromRequest()` 추가
- application service에서 Web Response 또는 Kafka message record 직접 조립

## 적용 절차

1. 현재 의존성을 먼저 검색한다.

   ```powershell
   rg "com.example.library.common.vo|new IDName|new Item|\.idName\(\)|\.item\(\)"
   ```

2. 현재 `AGENTS.md`가 `common-events/common/vo`를 허용하는 상태라면 `AGENTS.md`를 먼저 수정한다.
3. 각 서비스에 필요한 `domain/vo` record와 `application/dto` Command/Result record를 추가한다.
4. Web request 변환은 `adapter/in/web/dto`의 `toCommand()` 또는 `toQuery()`로 이동한다.
5. domain/application 코드에서 `common.vo` import를 제거한다.
6. `common-events` event/command/result record를 primitive 또는 simple snapshot field로 변경한다.
7. Kafka producer에서 local event/result를 `common-events` message로 변환한다.
8. Kafka consumer에서 shared message를 application command로 변환한다.
9. persistence mapper와 테스트 fixture를 local domain/application 타입 또는 primitive message field 기준으로 수정한다.
10. `common-events/src/main/java/com/example/library/common/vo`를 삭제한다.
11. 잔여 의존성을 확인한다.

   ```powershell
   rg "com.example.library.common.vo|common/vo|new IDName|new Item"
   ```

## 리뷰 체크리스트

- `common-events`에는 Kafka 계약만 있고 서비스 도메인 VO가 없다.
- domain/application 코드가 `com.example.library.common.vo`를 import하지 않는다.
- domain code가 `common-events`의 Kafka message record를 import하지 않는다.
- Web DTO, application DTO, domain VO, Kafka message, persistence entity/document 타입이 분리되어 있다.
- 변환 메서드는 상위 경계 객체 또는 adapter-local mapper에 있고, domain model이나 shared message record에 없다.
- Kafka consumer에는 최소 검증과 use case 위임을 넘어서는 비즈니스 판단이 없다.
- shared message contract를 바꿀 때 producer와 consumer를 같은 호환성 slice에서 함께 수정했다.
- 테스트와 fixture가 더 이상 `common-events`의 shared `IDName`, `Item`을 생성하지 않는다.

## 검증

작업 범위에 맞춰 targeted Gradle check를 우선 사용한다.

- 공유 계약을 바꾼 뒤에는 `.\gradlew.bat compileJava compileTestJava`를 실행한다.
- 한 서비스만 건드렸으면 해당 module-level test를 우선 실행한다.
- 실용적으로 가능하면 `.\gradlew.bat test`를 실행한다.
- 전체 테스트가 환경이나 DB 문제로 실패하면, 기존 실패와 이번 리팩터링 실패를 구분해서 보고한다.
