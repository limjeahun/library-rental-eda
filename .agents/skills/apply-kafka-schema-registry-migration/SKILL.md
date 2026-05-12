---
name: apply-kafka-schema-registry-migration
description: library-rental-eda에서 Kafka JSON/ObjectMapper 기반 메시지 계약을 Schema Registry 기반 계약으로 전환하거나 리뷰할 때 사용한다. Use when introducing Confluent Schema Registry, Avro/Protobuf/JSON Schema message contracts, generated Kafka message classes, schema compatibility rules, typed Kafka serializers/deserializers, or removing ObjectMapper.readValue from Kafka consumers. Coordinate with common-events contract boundaries, DDD/Hexagonal adapters, EDA/SAGA idempotency, and existing project exceptions. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# Kafka Schema Registry 전환

## 목적

이 스킬은 `library-rental-eda`의 Kafka 메시지를 현재의 JSON 문자열 + `ObjectMapper.readValue(...)` 방식에서 Schema Registry 기반 직렬화/역직렬화로 전환할 때 사용한다. 목표는 메시지 계약을 명시적 schema로 관리하고, Kafka consumer가 typed message를 받아 adapter 경계에서 application command로 변환하게 만드는 것이다.

## 필수 확인 문서

코드를 변경하기 전에 다음을 순서대로 확인한다.

1. `AGENTS.md`를 먼저 읽고 최우선 규칙으로 따른다.
2. `docs/hexagonal-eda-saga-review-improvements.md`에서 EDA/SAGA, consumer 멱등성, domain event와 integration message 분리 규칙을 확인한다.
3. `docs/common-events-vo-refactoring-plan.md`에서 Kafka message snapshot field와 common-events 경계 규칙을 확인한다.
4. Schema Registry 도입으로 `AGENTS.md`의 "common-events Java record" 규칙과 충돌하면, 첫 번째 slice에서 문서를 "schema + generated message contract"를 허용하도록 갱신한다.

문서가 충돌하면 `AGENTS.md`가 이긴다. 단, Schema Registry 도입 자체가 기존 문서와 충돌하는 경우에는 코드 변경 전에 문서 규칙을 먼저 명시적으로 업데이트한다.

## 범위

이 스킬의 범위는 다음과 같다.

- Schema Registry 컨테이너와 설정 추가
- `common-events`에 Avro/Protobuf/JSON Schema 파일 추가
- schema에서 Kafka message class 생성
- producer를 `JsonSerializer`에서 schema-aware serializer로 변경
- consumer를 `StringDeserializer` + `ObjectMapper`에서 schema-aware deserializer로 변경
- Kafka consumer에서 `ObjectMapper.readValue(...)` 제거
- adapter-local mapper로 generated message를 application command/result 또는 local event로 변환
- 기존 Redis processing lock과 DB `processed_messages` 멱등성 흐름 유지
- README, AGENTS, architecture docs의 메시지 계약 설명 갱신

다음은 도입하지 않는다.

- Outbox pattern
- DLQ, DLT, dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- `RestTemplate`, `WebClient`, OpenFeign 같은 직접 서비스 간 HTTP 호출

## 기본 선택

별도 요구가 없으면 Avro + Confluent Schema Registry를 기본안으로 둔다.

- Kafka 메시지 source of truth: `common-events/src/main/avro/*.avsc`
- 생성 클래스 패키지: 충돌 방지를 위해 우선 `com.example.library.common.event.schema`
- 기존 Java record와 같은 simple snapshot field 이름 유지: `eventId`, `correlationId`, `memberId`, `memberName`, `itemNo`, `itemTitle`, `point`
- 한 토픽에는 한 메시지 타입만 둔다. 여러 타입을 한 토픽에 섞지 않는다.
- Java type header에 의존하지 않는다. Schema Registry의 schema id와 subject compatibility를 사용한다.

Protobuf나 JSON Schema를 사용하라는 명시가 있으면 같은 단계 구조를 유지하되 serializer/deserializer, schema 파일 위치, code generation만 해당 기술에 맞춘다.

## 설계 규칙

- `common-events`는 공유 Kafka integration message 계약만 가진다. 서비스 도메인 VO를 넣지 않는다.
- generated schema class는 Kafka message contract이며 domain model이나 application DTO가 아니다.
- domain code는 generated schema class, Kafka, Schema Registry, Avro/Protobuf API를 import하지 않는다.
- 가능하면 application service도 generated schema class에 직접 의존하지 않는다. inbound adapter에서 application Command로 변환한다.
- Kafka producer는 `adapter/out/messaging`에서 local domain/application event를 generated message로 변환한다.
- Kafka consumer는 `adapter/in/messaging/consumer`에서 typed generated message를 받고 최소 검증, 멱등/lock 확인, application command 변환, use case 위임만 수행한다.
- service-to-service message에는 immutable snapshot field를 사용한다. `memberId`, `memberName`, `itemNo`, `itemTitle`처럼 서비스 경계를 넘는 값을 flat field로 둔다.
- schema evolution을 고려해 새 optional field에는 default 값을 둔다. 기존 required field 삭제나 타입 변경은 compatibility를 깨므로 피한다.
- protocol enum은 변경 빈도가 낮고 값 집합이 안정적일 때만 schema enum으로 둔다. 잦은 확장이 예상되면 string field + service-local enum 변환을 검토한다.
- JSON 메시지와 schema-aware binary 메시지를 같은 토픽에 섞지 않는다. 무중단 전환이 필요하면 v2 토픽을 사용한다.

## 사전 조사

변경 전에 현재 상태를 검색한다.

```powershell
rg -n "ObjectMapper|readValue|ConsumerRecord<String, String>|StringDeserializer|JsonSerializer|ADD_TYPE_INFO_HEADERS" book-service member-service rental-service bestbook-service
rg -n "record ItemRented|record ItemReturned|record EventResult|record PointUseCommand|enum EventType|enum Participant|enum SagaStep" common-events
rg -n "app.kafka.topics|rental-rent|rental-return|overdue-clear|rental-result|point-use" .
```

현재 메시지 타입과 토픽 매핑을 표로 정리한다.

| 토픽 | 기존 메시지 | 발행자 | 소비자 |
|------|-------------|--------|--------|
| `rental_rent` | `ItemRented` | `rental-service` | `book-service`, `member-service`, `bestbook-service` |
| `rental_return` | `ItemReturned` | `rental-service` | `book-service`, `member-service` |
| `overdue_clear` | `OverdueCleared` | `rental-service` | `member-service` |
| `rental_result` | `EventResult` | `book-service`, `member-service` | `rental-service` |
| `point_use` | `PointUseCommand` | `rental-service` | `member-service` |
| cancel topics | cancel event records | `rental-service` | `book-service`, `bestbook-service` |

## 적용 절차

### 1. 문서와 migration strategy 결정

1. 로컬/학습 목적이면 producer와 consumer를 한 번에 바꾸는 big-bang 전환을 사용할 수 있다.
2. 기존 배포와 호환이 필요하면 `*_v2` 토픽을 만들고 JSON 토픽과 schema 토픽을 분리한다.
3. `README.md`, `AGENTS.md`, 개선 문서에서 "JSON 문자열 + ObjectMapper" 설명을 Schema Registry 방식으로 갱신할 범위를 정한다.

### 2. Schema Registry 런타임 추가

1. Docker compose에 Schema Registry 서비스를 추가한다.
2. 서비스별 `application.yml`에 `spring.kafka.properties.schema.registry.url`을 추가한다.
3. 로컬과 컨테이너 내부 주소를 구분한다. 컨테이너 내부에서는 compose service name을 사용한다.

### 3. Gradle 설정 추가

1. `settings.gradle.kts`가 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`를 사용하므로 필요한 외부 Maven repository는 `dependencyResolutionManagement.repositories`에 추가한다.
2. `common-events`에 schema code generation plugin과 Avro/Protobuf runtime dependency를 추가한다.
3. Kafka를 사용하는 서비스에는 schema-aware serializer/deserializer dependency를 추가한다.
4. generated source directory가 `compileJava`에 포함되는지 확인한다.
5. 버전은 Spring Boot, Spring Kafka, Kafka client, Schema Registry 호환성을 확인한 뒤 명시적으로 pinning한다.

### 4. common-events schema 작성

1. 기존 Java record 필드를 기준으로 schema 파일을 만든다.
2. 서비스 도메인 VO 구조를 schema에 넣지 않고 flat snapshot field를 유지한다.
3. `eventId`와 `correlationId`는 모든 event/command/result schema에 유지한다.
4. `EventResult`에는 `sourceEventId`, `participant`, `step`, `eventType`, `successed`, `reason` 등 현재 계약 필드를 빠뜨리지 않는다.
5. optional field에는 null/default 또는 명시 default를 둔다.
6. 기존 Java record와 class name 충돌이 있으면 generated namespace를 `com.example.library.common.event.schema`로 둔다.

### 5. Outbound producer 변경

1. `adapter/out/messaging`에 domain/application event -> generated schema message mapper를 둔다.
2. `JsonSerializer` 설정을 schema-aware serializer로 바꾼다.
3. `KafkaTemplate<String, Object>`를 유지하거나, 서비스별로 더 좁은 typed template를 둘 수 있다. 기존 패턴과 충돌하지 않는 쪽을 선택한다.
4. producer에서 `eventId`는 발행 메시지마다 새로 만들고, `correlationId`는 기존 흐름 값을 보존한다.
5. producer가 Java type header에 의존하지 않게 한다.

### 6. Inbound consumer 변경

1. `StringDeserializer` + `ConsumerRecord<String, String>`를 schema-aware deserializer + typed payload로 바꾼다.
2. `ObjectMapper.readValue(...)`를 제거한다.
3. typed generated message에서 `eventId`를 꺼내 Redis processing lock을 획득한다.
4. lock 획득 실패는 `ALREADY_PROCESSING`으로 보고 로그 후 return한다.
5. lock 획득 후 use case 처리 중 예외가 나면 Redis processing key를 삭제하고 예외를 다시 던진다.
6. generated message를 application command로 변환해 use case에 넘긴다. application service에 generated schema class를 직접 전달하는 방식은 중간 단계에서만 허용하고 최종 상태에서는 제거한다.

권장 consumer 형태:

```java
@KafkaListener(
    topics = "${app.kafka.topics.rental-rent}",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void consumeRent(ItemRentedMessage message) {
    String eventId = message.getEventId();

    switch (tryAcquireProcessingLock(eventId)) {
        case CLAIMED -> {
            try {
                handleMemberEventUseCase.handleRent(toCommand(message));
            } catch (Exception ex) {
                releaseProcessing(eventId);
                throw ex;
            }
        }
        case ALREADY_PROCESSING -> {
            log.info("skip already processing member rent eventId={}", eventId);
            return;
        }
    }
}
```

### 7. Application boundary 정리

1. `HandleMemberEventUseCase`, `BookRentalEventService`, `RentalResultService`, `BestBookService`가 shared generated message를 직접 받는지 확인한다.
2. 가능하면 `application/dto`에 primitive/simple field 기반 Command를 만들고 consumer가 변환한다.
3. application service 안의 `messageIdempotencyPort.markProcessed(eventId, correlationId, InboundMessageType...)` 흐름은 유지한다.
4. domain model이 generated message를 import하지 않는지 확인한다.

### 8. 기존 record 정리

1. 모든 producer/consumer가 generated message로 이동한 뒤 기존 `common-events` Java record 사용처를 검색한다.
2. 더 이상 Kafka 계약으로 쓰지 않는 record는 제거하거나, 내부 테스트 fixture로 남기지 않는다.
3. protocol enum을 schema enum으로 옮긴 경우 Java enum 잔여 사용처와 변환 정책을 정리한다.

### 9. 운영 compatibility 설정

1. Schema Registry subject naming strategy를 확인한다. 한 토픽 한 타입이면 기본 topic-value subject 전략이 단순하다.
2. 스키마 호환성은 최소 BACKWARD 또는 팀 정책에 맞게 설정한다.
3. 필드 추가/삭제/rename 규칙을 README나 docs에 남긴다.
4. JSON 토픽에서 schema 토픽으로 전환하는 경우 v2 토픽 cutover 순서와 rollback 방법을 문서화한다.

## 검증 체크리스트

- `common-events` schema generation이 성공한다.
- producer가 Schema Registry에 subject를 등록한다.
- consumer에서 `ObjectMapper.readValue(...)`가 제거되었다.
- `spring.json.*` 설정이나 Java type header 의존이 남아 있지 않다.
- domain code가 schema generated class, Kafka, Avro/Protobuf API를 import하지 않는다.
- application code가 generated message class에 직접 의존하지 않거나, 남은 경우 후속 slice로 명시되어 있다.
- consumer Redis processing lock과 DB `processed_messages` 멱등성 기록이 유지된다.
- schema 메시지는 service-local domain VO가 아니라 primitive/simple snapshot field를 가진다.
- JSON 메시지와 schema 메시지를 같은 토픽에 섞지 않는다.
- README의 Kafka 메시지 설명이 실제 구현과 일치한다.

## 검증 명령

작업 범위에 따라 targeted check를 우선 실행한다.

```powershell
.\gradlew.bat :common-events:compileJava
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat :common-events:test --tests com.example.library.common.architecture.CommonEventsArchitectureTest
.\gradlew.bat :rental-service:test --tests com.example.library.rental.architecture.HexagonalArchitectureTest
.\gradlew.bat :book-service:test --tests com.example.library.book.architecture.HexagonalArchitectureTest
.\gradlew.bat :member-service:test --tests com.example.library.member.architecture.HexagonalArchitectureTest
.\gradlew.bat :bestbook-service:test --tests com.example.library.bestbook.architecture.HexagonalArchitectureTest
```

가능하면 Docker compose로 Kafka와 Schema Registry를 띄우고 최소 publish/consume smoke test를 수행한다. 환경 의존 실패는 코드 실패와 구분해서 보고한다.

## 흔한 함정

- `spring.json.value.default.type`로 여러 메시지 타입을 처리하려고 하지 않는다. Schema Registry 전환 후에는 JSON deserializer 설정이 아니라 schema-aware deserializer를 사용한다.
- generated Avro class를 domain model처럼 사용하지 않는다.
- Avro enum 값 변경은 호환성 문제가 될 수 있다. 새 enum 값을 자주 추가해야 하면 string + adapter-local enum 변환을 검토한다.
- nullable field에 default를 빼먹으면 이전 consumer와 compatibility 문제가 생긴다.
- 기존 Java record와 generated class package/name 충돌을 방치하지 않는다.
- Schema Registry 장애 시 producer/consumer 시작과 메시지 처리 동작이 달라질 수 있으므로 로컬 smoke test로 확인한다.
