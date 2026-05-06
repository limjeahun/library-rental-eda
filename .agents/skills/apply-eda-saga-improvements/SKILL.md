---
name: apply-eda-saga-improvements
description: library-rental-eda에서 docs/hexagonal-eda-saga-review-improvements.md의 Hexagonal Architecture, DDD, EDA/SAGA 개선안을 코드에 적용하거나 리뷰할 때 사용한다. Use when implementing or reviewing consumer idempotency, EventResult eventId/sourceEventId/participant/step, SAGA participant result tracking, compensation idempotency, compensation completion events, domain event vs integration message separation, application config dependency removal, or rental-service DTO separation. Trigger phrases include "P0 멱등성", "EventResult 개선", "SAGA 결과 추적", "보상 멱등성", "도메인 이벤트 분리". Do not use for unrelated UI changes, non-EDA refactors, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# EDA SAGA 개선 적용

## 목적

이 스킬은 `library-rental-eda`의 리뷰 개선안을 안전하고 점진적인 Java/Spring 코드 변경으로 옮길 때 사용한다. 작업 중에는 DDD, Hexagonal Architecture, Kafka-only 서비스 경계, 프로젝트 예외 규칙을 함께 지킨다.

## 필수 읽기

코드를 변경하기 전에 다음 문서를 읽는다.

1. `AGENTS.md`를 먼저 읽고 최우선 규칙으로 따른다.
2. `docs/hexagonal-eda-saga-review-improvements.md`를 읽고 개선안의 source of truth로 삼는다.
3. 패키지 배치, 네이밍, 레이어 책임이 애매할 때만 `docs/architecture-rule-eda.md`를 추가로 참고한다.

문서가 충돌하면 `AGENTS.md`가 이긴다.

## 범위 제한

다음 항목은 도입하지 않는다.

- Outbox pattern
- DLQ, DLT, or dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- Direct service-to-service HTTP calls such as `RestTemplate`, `WebClient`, or OpenFeign

서비스 간 상태 변경은 Kafka command/event/result 흐름으로 유지한다. 서비스 간 조회는 사용자가 별도 설계를 명시하지 않는 한 event-maintained local read model을 우선한다.

## 작업 흐름

1. 사용자가 요청한 개선 항목을 먼저 분류한다: P0 Consumer 멱등성, Result Event 계약, SAGA 결과 추적, 보상 멱등성, 보상 완료 이벤트, 도메인 이벤트와 통합 메시지 분리, config 의존 제거, DTO 분리.
2. 수정 전에 `rg`로 현재 구현을 확인한다. 영향받는 서비스 모듈과 `common-events`를 함께 검색한다.
3. 변경은 점진적으로 적용한다. 큰 요청이면 `docs/hexagonal-eda-saga-review-improvements.md`의 권장 적용 순서를 따르고, 한 번에 하나의 일관된 slice를 끝낸다.
4. 여러 서비스가 공유하는 메시지 계약은 `common-events`에 둔다. 서비스 고유 비즈니스 상태와 도메인 이벤트는 소유 서비스 내부에 둔다.
5. Consumer는 얇게 유지한다. 역직렬화, 최소 검증, 멱등/점유 확인 후 application use case에 위임한다.
6. 비즈니스 판단, 보상 판단, 상태 전이는 Kafka consumer나 config가 아니라 application/domain 코드에 둔다.
7. 바뀐 동작에 맞는 집중 테스트를 추가하거나 수정한다. 범위가 한 모듈이면 module-level Gradle test를 우선하고, 공유 계약이나 여러 모듈을 건드리면 `.\gradlew.bat test`를 검토한다.

## 패키지 배치

기존 코드나 `AGENTS.md`가 더 좁은 조정을 요구하지 않는 한 아래 배치를 따른다.

- Kafka consumers: `adapter/in/messaging/consumer`
- Kafka producers: `adapter/out/messaging`
- JPA entities, repositories, persistence mappers: `adapter/out/persistence`
- MongoDB documents and repositories in `bestbook-service`: `adapter/out/persistence`
- Inbound ports: `application/port/in`
- Outbound ports: `application/port/out`
- Use case orchestration: `application/service`
- Pure business models and enums: `domain/model`
- Simple immutable value objects: `domain/vo`
- Spring configuration and properties: `config`
- Shared events, commands, result contracts, and protocol enums: `common-events/src/main/java/com/example/library/common/event` or `common/.../vo`

domain과 application 코드는 adapter 패키지에 의존하지 않는다. domain 코드는 Spring, JPA, MongoDB, Kafka, Redis, web framework에 의존하지 않는다.

## 메시지 계약 규칙

기존 코드와 충돌하지 않으면 공유 event, command, result, 단순 value 계약은 Java record를 사용한다.

Result Event에는 다음 규칙을 적용한다.

- 발행되는 Result Event마다 새 `eventId`를 생성한다.
- 원본 메시지 식별자는 `sourceEventId`에 보존한다.
- 하나의 비동기 비즈니스 흐름 안에서는 `correlationId`를 계속 보존한다.
- 결과 해석이 참여자나 SAGA 단계에 따라 달라지면 participant와 step 정보를 포함한다.
- record는 `eventId()`, `correlationId()` 같은 canonical accessor로 접근한다. record에 JavaBean getter를 추가하지 않는다.

실패 Result Event에서 보상 command를 발행할 때는 command의 새 `eventId`를 만들고 원래 `correlationId`를 보존한다.

## Consumer 멱등성 규칙

동시 처리 방지와 durable processed-message 상태를 분리한다.

- Redis `SETNX`/`setIfAbsent` 방식 claim은 짧은 processing lock으로만 사용한다.
- 정합성이 중요한 경우 durable idempotency source는 서비스 소유 DB의 processed message store로 둔다.
- 가능하면 비즈니스 상태 변경과 processed message record 저장을 같은 트랜잭션으로 처리한다.
- processed message에는 `eventId` 또는 `serviceName + eventId` unique constraint/index를 둔다.
- unique constraint 위반은 이미 처리된 메시지로 보고 비즈니스 로직을 실행하지 않는다.
- 처리 실패 시 processing claim을 해제해 Kafka redelivery가 다시 처리할 수 있게 한다.

분기 의미는 `CLAIMED`, `ALREADY_PROCESSING`, `ALREADY_PROCESSED`, `FAILED_AND_RELEASED`처럼 명시적으로 구분한다.

## SAGA 결과 추적 규칙

RENT/RETURN/OVERDUE 결과 처리에는 다음 규칙을 적용한다.

- 참여 서비스는 성공/실패 Result Event를 모두 발행하게 한다.
- 참여자 결과는 `EventType`만이 아니라 `correlationId` 기준으로 추적한다.
- 여러 참여자가 얽힌 흐름에서 구분되지 않은 단일 실패 Result만 보고 즉시 보상을 실행하지 않는다.
- 성공이 확인된 참여자만 보상한다.
- 참여자 결과가 pending이면, 흐름 실패 이후 늦은 성공 Result가 도착했을 때 보상할 수 있도록 로컬 상태를 남긴다.
- 사용자가 별도 timeout 보상 설계를 명시하지 않는 한 timeout은 자동 보상이 아니라 local monitoring과 `NEEDS_MANUAL_REVIEW`로 먼저 다룬다.

## 보상 규칙

메시지 멱등성과 비즈니스 보상 멱등성을 분리한다.

- 메시지 멱등성 키는 들어온 Result Event의 `eventId`를 기준으로 둔다.
- 보상 멱등성 키는 `correlationId + 명시적 compensation type`을 기준으로 둔다. Result의 참여자/단계와 보상이 깔끔하게 대응하면 `correlationId + eventType + participant + step`도 사용할 수 있다.
- aggregate 보상 메서드는 이미 보상된 상태에서도 안전하게 동작하게 한다.
- downstream read model이 이전 반영을 되돌려야 하면 보상 완료 이벤트를 발행한다.

bestbook read model 정합성을 위해 rental 보상이 MongoDB read model에 반영되어야 하면 `ItemRentCanceled`, `ItemReturnCanceled`, `OverdueClearCanceled` 같은 이벤트를 검토한다.

## 도메인 이벤트와 통합 메시지 분리

aggregate가 `common-events`의 공유 Kafka message record를 직접 생성하지 않게 한다.

권장 흐름은 다음과 같다.

1. Aggregate는 순수 service-local domain event/result를 만들거나 반환한다.
2. Application service는 aggregate 상태를 저장하고 outbound port를 호출한다.
3. Messaging adapter는 domain event/result를 `common-events` 통합 메시지로 변환한다.
4. Messaging adapter는 `eventId`, `correlationId`, `occurredAt` 같은 통합 메시지 메타데이터를 부여한다.

순수 domain model에는 Spring `@DomainEvents`를 붙이지 않는다. 이 방식은 domain code를 Spring Data에 결합시킨다.

## 검증 체크리스트

수정한 slice와 관련된 항목을 확인한다.

- 같은 `eventId`가 동시에 전달되어도 비즈니스 로직은 두 번 실행되지 않는다.
- 처리 실패가 retry를 막는 durable processed-message record를 남기지 않는다.
- 필요한 곳에서는 DB 상태와 processed-message record가 같은 트랜잭션으로 커밋된다.
- Result Event는 새 `eventId`, 보존된 `sourceEventId`, 보존된 `correlationId`, participant, step을 포함한다.
- book/member 서비스는 참여 흐름에서 성공/실패 Result Event를 모두 발행한다.
- rental-service는 book 참여 결과와 member 참여 결과를 분리 저장하고 해석한다.
- 여러 실패 Result가 도착해도 같은 비즈니스 보상 키에 대한 보상은 한 번만 실행된다.
- 보상 완료 이벤트가 read model을 일관되게 갱신한다.
- 도메인/통합 이벤트 분리를 적용했다면 `rental-service` domain 코드가 `com.example.library.common.event`를 import하지 않는다.
- dependency cleanup을 적용했다면 `application` 패키지가 adapter나 config 패키지를 import하지 않는다.
