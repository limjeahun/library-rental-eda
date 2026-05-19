---
name: architecture-super-agent
description: library-rental-eda에서 더 엄격한 아키텍처 규칙을 적용하거나 전체 구조를 점검할 때 사용하는 상위 조정 스킬이다. Use when the user asks for "Super Agent", "architecture super agent", "전체 아키텍처 점검", "아키텍처 규칙 적용", "완벽한 아키텍처", or when coordinating multiple project-specific refactoring skills. It triages DDD/Hexagonal/EDA violations, runs the local architecture scan, chooses the right specialized skill such as apply-eda-saga-improvements, apply-common-events-vo-refactoring, apply-web-request-command-domain-vo-boundary, or apply-class-constant-removal-refactoring, and applies changes in safe vertical slices. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# Architecture Super Agent

## 목적

이 스킬은 `library-rental-eda`의 상위 아키텍처 감독자 역할을 한다. 하나의 구체 리팩터링만 바로 적용하기보다, 현재 요청과 코드 상태를 먼저 분류하고 가장 알맞은 프로젝트 전용 스킬 또는 직접 적용 절차로 연결한다.

## 필수 우선순위

작업 전에 다음 순서를 지킨다.

1. `AGENTS.md`를 최우선 규칙으로 읽고 따른다.
2. 전체 구조나 네이밍 판단이 필요할 때 `docs/architecture-rule-eda.md`를 참고한다.
3. 특정 개선 slice에 들어가면 해당 계획 문서를 source of truth로 읽는다.
4. 문서가 충돌하면 `AGENTS.md`가 이긴다.

## 시작 절차

1. 요청을 먼저 분류한다: 전체 진단, 코드 변경, 리뷰, 특정 리팩터링 적용, 규칙 문서 보강.
2. 작은 단일 파일 요청이 아니라면 `scripts/Invoke-ArchitectureScan.ps1 -Root .`를 실행해 위반 후보를 수집한다.
3. 스캔 결과는 단서로만 취급한다. false positive 가능성이 있으므로 관련 코드를 직접 확인한 뒤 판단한다.
4. 변경이 domain/application/adapter 경계를 건드리면 해당 모듈의 `HexagonalArchitectureTest`가 통과해야 한다.
5. `common-events` 계약을 건드리면 `CommonEventsArchitectureTest`가 통과해야 한다.
6. 변경이 필요하면 한 번에 하나의 vertical slice를 끝낸다. 공유 계약, producer, consumer, test fixture를 함께 바꿔야 하는 slice는 같은 변경 단위로 묶는다.
7. 프로젝트 예외를 새로 도입하지 않는다: Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, SAGA orchestration, direct service-to-service HTTP calls.

## 세부 스킬 라우팅

위반 유형에 따라 다음 스킬을 사용한다.

- `common-events`에 service domain VO가 있거나 `IDName`, `Item` 같은 공유 VO 제거가 필요하면 `$apply-common-events-vo-refactoring`.
- Web Request DTO, application Command/Query, domain VO 경계가 섞였으면 `$apply-web-request-command-domain-vo-boundary`.
- Consumer 멱등성, EventResult 계약, SAGA participant/step 추적, 보상 멱등성, 도메인 이벤트와 통합 메시지 분리가 필요하면 `$apply-eda-saga-improvements`.
- Application service class-level constants, magic strings, domain policy values, compensation/idempotency keys 정리가 필요하면 `$apply-class-constant-removal-refactoring`.
- Domain aggregate root 생성 경로, public constructor 제거, Lombok accessor 제거, 명시적 accessor, aggregate 내부 snapshot 기반 domain event, `pullDomainEvents()` clear, 내부 컬렉션 방어적 반환 정리가 필요하면 `$apply-domain-aggregate-root-rules`.
- 위 전용 스킬에 딱 들어맞지 않는 일반 계층 의존성, 패키지 배치, DTO 노출 문제는 `AGENTS.md`와 `docs/architecture-rule-eda.md` 기준으로 직접 처리한다.

## 우선순위 기준

먼저 P0를 처리한다.

- P0: domain/application dependency 방향 위반, direct service HTTP client, `common-events` domain VO 재사용, unsafe idempotent consumer, 잘못된 Kafka message contract, 보상 중복 실행 위험.
- P1: Web DTO가 domain VO를 생성, application service에 business/protocol constant 존재, controller가 response DTO를 수동 합성, adapter 책임이 application/domain으로 새어 들어감.
- P2: 패키지 네이밍 불일치, 테스트 fixture의 낡은 타입 사용, 중복 변환 helper, 문서와 코드 사이의 경미한 표현 차이.

## 검토 체크리스트

변경 전후로 관련 항목만 확인한다.

- Domain code가 Spring, JPA, MongoDB, Kafka, Redis, web framework, adapter, config, `common-events` integration message에 의존하지 않는다.
- Application code가 adapter/config/web/kafka/persistence 구현에 의존하지 않는다.
- Controllers는 request validation, command/query 변환, use case 호출, response wrapping만 담당한다.
- Web DTO, application DTO, domain VO/model, persistence entity/document, Kafka message contract가 분리되어 있다.
- Web Request DTO와 adapter-facing application Command는 domain VO/model을 직접 들고 다니지 않는다.
- Shared Kafka record는 primitive/simple snapshot field와 shared protocol enum 중심으로 표현된다.
- Kafka consumer는 최소 검증과 application use case 위임을 넘는 비즈니스 판단을 하지 않는다.
- Message idempotency key와 business compensation idempotency key가 분리되어 있다.
- Finite business/protocol concept은 string constant가 아니라 enum 또는 value object로 표현된다.

## 검증

범위에 맞춰 가장 작은 검증부터 실행한다.

```powershell
.\.agents\skills\architecture-super-agent\scripts\Invoke-ArchitectureScan.ps1 -Root .
.\gradlew.bat :<module>:test --tests com.example.library.<service>.architecture.HexagonalArchitectureTest
.\gradlew.bat :common-events:test --tests com.example.library.common.architecture.CommonEventsArchitectureTest
.\gradlew.bat :<module>:test
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test
```

스캔이 경고를 출력해도 즉시 실패로 간주하지 않는다. 현재 작업과 관련된 경고를 확인하고, 의도적으로 남긴 기존 구조라면 최종 보고에 잔여 리스크로 남긴다. ArchUnit 실패는 자동화된 아키텍처 회귀로 보고, 규칙이 잘못된 경우가 아니라면 코드를 고쳐 통과시킨다.
