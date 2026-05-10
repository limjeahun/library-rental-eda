# Library Rental EDA

## Instruction Priority

1. Follow this `AGENTS.md` first.
2. Use `docs/architecture-rule-eda.md` as a reference for DDD, Hexagonal Architecture, DTO separation, ports/adapters, and EDA naming patterns.
3. If `docs/architecture-rule-eda.md` conflicts with this file, this file wins.
4. Preserve the target project structure and naming in this file unless the user explicitly asks for a refactor.

## Architecture Super Agent

- For broad architecture requests, "Super Agent" requests, or whole-project rule application, use the project-local `.agents/skills/architecture-super-agent` skill first.
- Start broad checks with `.\.agents\skills\architecture-super-agent\scripts\Invoke-ArchitectureScan.ps1 -Root .` unless the task is a small single-file change.
- Treat scan output as review leads, not automatic truth. Confirm each finding against this `AGENTS.md` before editing code.
- Enforce stable architecture rules with the ArchUnit tests under each module's `src/test/java/.../architecture`.
- When changing domain/application/adapter boundaries, run the relevant module's `HexagonalArchitectureTest`; when changing `common-events`, run `CommonEventsArchitectureTest`.
- Route concrete slices to the existing project skills: EDA/SAGA improvements, common-events VO refactoring, web request/command/domain VO boundary, or class constant removal.

## Project Stack

- Java application code is written in Java, not Kotlin.
- The project uses Java 21, Gradle Wrapper 8.5, Spring Boot 3.3.7, MariaDB, MongoDB, Redis, and Kafka KRaft.
- `book-service`, `member-service`, and `rental-service` use MariaDB with Spring Data JPA.
- `bestbook-service` is an event-maintained read model and uses MongoDB with Spring Data MongoDB.
- The Gradle root project is `library-rental-eda`.
- Current modules are:
  - `common-core`
  - `common-events`
  - `book-service`
  - `member-service`
  - `rental-service`
  - `bestbook-service`

## Project Exceptions

Do not implement the following, even if the reference architecture document mentions them:

- Outbox pattern
- DLQ, DLT, or dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- Direct service-to-service HTTP calls

Use normal Spring Boot, Spring Kafka, JPA, MongoDB, Redis, and Gradle patterns already present in the project.

## Service Boundary Rules

- Keep service boundaries strict.
- Service-to-service communication must use Kafka event or command messaging only.
- Do not add `RestTemplate`, `WebClient`, OpenFeign, or other direct HTTP clients for service-to-service communication.
- Cross-service state changes must be modeled as Kafka command/event flows.
- Cross-service reads must not be implemented with direct HTTP calls. Prefer event-maintained local read models or ask for the intended design when unclear.
- Shared event, command, result, and protocol enum contracts belong in `common-events` when they are used by more than one service.
- Do not place service domain value objects in `common-events`.
- When a Kafka message needs member, item, or other cross-service data, model it as immutable snapshot fields in the message contract, such as `memberId`, `memberName`, `itemNo`, and `itemTitle`.

## Architecture Principles

- Use DDD and Hexagonal Architecture as the default design style.
- Domain code should contain business rules and stay independent from Spring, JPA, MongoDB, Kafka, Redis, and web frameworks.
- Application code should orchestrate use cases and depend on domain abstractions.
- Adapters should handle web, Kafka, persistence, Redis, and other technical integration concerns.
- Do not expose domain entities directly from controllers.
- Do not put persistence annotations on pure domain models unless the existing code already uses that pattern and the task is explicitly scoped to preserve it.
- Keep inbound ports, outbound ports, use cases, domain models, and adapters separated.
- Prefer Java `record` for DTOs, commands, events, and simple immutable message payloads when compatible with the existing code.
- Keep simple immutable domain value objects under each service's `domain/vo` and prefer Java `record` for them.
- Application DTOs under `application/dto` are use case Command, Query, and Result records; they are not a replacement location for domain value objects.
- Do not reuse `common-events` message payload types as service domain value objects or application DTOs.
- Service-local domain events belong in each service's `domain/event` as top-level Java records.
- Do not group domain events in a generic holder class when each event is part of the service's domain language.
- Service-local domain events must not contain Kafka metadata or depend on `common-events`; messaging adapters convert them to shared integration messages.
- Keep child domain models that participate in aggregate state transitions under `domain/model`, even when they are implemented as Java records.
- Domain enums that express business state or business classification belong in `domain/model`.
- Shared message/protocol enums used across services belong in `common-events`, and adapter-only or persistence-only enums should stay in the relevant adapter package.
- Use record canonical accessors such as `id()`, `item()`, and `correlationId()` for records. Do not add JavaBean compatibility getters or setters to records.
- Existing aggregate/domain model getters such as `RentalCard.getRentItemList()` and persistence entity getters may remain when they are part of the current API or framework mapping style.

## Policy, Protocol Key, and Constants Rules

- Do not use application services or use case services as a place to hold class-level constants for domain policy values, compensation/idempotency keys, event or command type strings, Kafka protocol keys, or cross-service workflow identifiers.
- Avoid introducing `private static final` constants in service classes. A service class should orchestrate a use case, not define business policy or protocol vocabularies.
- Domain policy values such as rent points, return points, overdue rules, or rental limits belong in domain model behavior, a domain policy object, a domain value object, or a domain enum with behavior.
- Do not turn magic strings into string constants when the value represents a finite business or protocol concept. Use a typed enum or value object instead so the compiler can catch invalid values.
- Compensation, idempotency, and workflow step keys should be represented by typed service-local enums or value objects. If the key is part of an inter-service message protocol, define the shared protocol type in `common-events`.
- Runtime-configurable technical values belong in Spring configuration properties under `config`, then flow into adapters or application services through explicit dependencies.
- Local constants are allowed only when they are purely private implementation details with no business, protocol, persistence, messaging, or cross-service meaning. Keep them close to the code that uses them and avoid promoting them to service-level vocabulary.

## Target Package Structure

For `book-service`, `member-service`, `rental-service`, and `bestbook-service`, follow the Architecture Rule target structure:

```text
com.example.library.{service}/
├── application/
│   ├── dto/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── service/
├── config/
├── domain/
│   ├── event/
│   ├── model/
│   └── vo/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── dto/
│   │   └── messaging/
│   │       └── consumer/
│   └── out/
│       ├── messaging/
│       └── persistence/
```

For `common-events`, keep shared contracts under:

```text
com.example.library.common/
└── event/
```

Do not create `common.vo` for service domain value objects.
If a message-only nested value type is truly necessary, place it under a message-specific package or name it explicitly as a message payload type, and do not use it from service domain models.

## DTO and Conversion Rules

- Web request and response DTOs belong in `adapter/in/web/dto`.
- Application Command, Query, and Result records belong in `application/dto`.
- Domain value objects belong in each service's `domain/vo`, not in `application/dto`.
- Web Request DTOs may convert to application Command or Query with `toCommand()` or `toQuery()`.
- Web Request DTOs should convert to application Command or Query using primitive or simple request fields, not service domain value objects.
- Web Response DTOs may convert from application Result with `from(result)`.
- Application Result records may convert from domain models with `from(domain)` when useful.
- Application services should create service domain value objects from application Command or Query values immediately before invoking domain models.
- Application Command and Query records should prefer primitive or simple use-case input fields over domain value object fields unless the use case is purely internal and has no adapter-facing input.
- Persistence Entity/Document and domain model conversion belongs in `adapter/out/persistence` mapper classes.
- Shared Kafka message records from `common-events` must not define service-specific conversion methods such as `toRentalCommand()` or `toMemberCommand()`.
- Kafka consumers or adapter-local messaging mappers convert shared Kafka messages to application commands.
- Kafka producers or adapter-local messaging mappers convert local domain/application events or results to shared Kafka messages.
- Domain models and domain value objects must not define `toResponse()`, `toCommonEvent()`, or `toJpaEntity()`.
- Application Command records must not define `fromRequest()` because that would make application DTOs depend on adapter DTOs.
- Inbound web and messaging adapters should not expose methods such as `toIdName()`, `toItem()`, or `toDomainVo()` on request/message DTOs.

## Web Response DTO Rules

- Controllers should stay thin: validate the request, convert the request DTO to a Command or Query, call the use case, convert the application Result to the final response DTO, and wrap it in `BaseResponse`.
- Controllers should not manually build one response DTO and pass it into another response DTO just to create the final response.
- Response DTO factory methods should accept application Result records or primitive/simple values as input.
- Avoid response factory methods that require another response DTO as an input, such as `RentalResultResponse.of(message, RentalCardResponse.from(result))`.
- Prefer intent-revealing response factory methods such as `RentalResultResponse.rentAccepted(result)`, where the response DTO owns the final HTTP response shape.
- A response DTO may contain nested response-shaped fields only when that nested JSON object is part of the public API contract. Even then, conversion should start from application Result records, not from a response DTO already created by the controller.
- HTTP response messages such as "요청을 접수했습니다." belong in web response DTO factories or web adapter-local helpers, not in controllers, application services, domain models, or application Result records.
- Application Result records should contain use case result data, not presentation-only HTTP messages. Add a separate application result only when the message represents real use case state, protocol status, or correlation data.
- When a response DTO needs the same application Result mapping in several factory methods, keep the shared mapping in a private helper inside the response DTO.
- Before finishing web adapter changes, check whether any controller composes response DTOs manually, whether any response DTO factory unnecessarily accepts another response DTO, and whether response messages expose Kafka/event implementation details.

Bad:

```java
RentalResultResponse.of(
    "도서 대여 요청을 접수했습니다.",
    RentalCardResponse.from(rentItemUseCase.rentItem(command))
)
```

Good:

```java
RentalResultResponse.rentAccepted(
    rentItemUseCase.rentItem(command)
)
```

## EDA Rules

- Distinguish command messages, domain events, and result events by name and purpose.
- Command messages express requested actions.
- Domain events express facts that already happened.
- Result events express command processing outcomes.
- Service-local domain events belong under `domain/event`; shared Kafka integration events belong under `common-events`.
- Shared event, command, and result contracts in `common-events` should be Java records and should be accessed with record accessors.
- Prefer primitive or simple snapshot fields in shared Kafka message records over shared domain VO fields.
- Service-local domain/application code should convert to and from shared Kafka message records at adapter boundaries.
- Use `eventId` as the unique message identifier for idempotent consumer checks.
- Use `correlationId` to tie one asynchronous business flow together across domain events, result events, and compensation commands.
- When publishing a compensation command from a failed result event, create a new `eventId` for the command and preserve the original `correlationId`.
- Kafka consumers should live in `adapter/in/messaging/consumer`.
- Kafka producers should live in `adapter/out/messaging` and implement outbound event ports from `application/port/out`.
- Persistence adapters should live in `adapter/out/persistence` and implement repository/output ports from `application/port/out`.
- In `bestbook-service`, persistence adapters should use MongoDB `@Document` models and `MongoRepository`, not JPA entities or `JpaRepository`.
- Consumers should deserialize, validate minimally, and delegate to application use cases.
- Business decisions should not live in Kafka consumer classes.

## Config and Technical Support Rules

- This project does not use a dedicated `infrastructure` package by default.
- Spring `@Configuration`, configuration properties, and technical bean wiring belong in `config`.
- Small technical support classes that exist only to support configuration may live under `config` or a clear `config` subpackage.
- Do not introduce `infrastructure/messaging` unless the project structure is explicitly redesigned.
- Kafka consumers live in `adapter/in/messaging/consumer`.
- Kafka producers live in `adapter/out/messaging`.
- JPA entities, MongoDB documents, persistence mappers, and Spring Data repositories live in `adapter/out/persistence`.
- Domain and application code must not depend on `config` or adapter packages.
- Do not put business rules, compensation decisions, use case orchestration, or service-to-service workflow logic in `config`.

## Validation

- Prefer targeted Gradle checks while developing.
- For architecture-sensitive changes, run the matching ArchUnit test, for example `.\gradlew.bat :rental-service:test --tests com.example.library.rental.architecture.HexagonalArchitectureTest`.
- For `common-events` contract changes, run `.\gradlew.bat :common-events:test --tests com.example.library.common.architecture.CommonEventsArchitectureTest`.
- Use `.\gradlew.bat test` for full verification when practical.
- Use module-level tests when the change is scoped to one service.
