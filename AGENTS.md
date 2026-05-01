# Library Rental EDA

## Instruction Priority

1. Follow this `AGENTS.md` first.
2. Use `docs/architecture-rule-eda.md` as a reference for DDD, Hexagonal Architecture, DTO separation, ports/adapters, and EDA naming patterns.
3. If `docs/architecture-rule-eda.md` conflicts with this file, this file wins.
4. Preserve the target project structure and naming in this file unless the user explicitly asks for a refactor.

## Project Stack

- Java application code is written in Java, not Kotlin.
- The project uses Java 21, Gradle Wrapper 8.5, Spring Boot 3.3.7, MariaDB, MongoDB, Redis, and Kafka KRaft.
- `book-service`, `member-service`, and `rental-service` use MariaDB with Spring Data JPA.
- `bestbook-service` is an event-maintained read model and uses MongoDB with Spring Data MongoDB.
- The Gradle root project is `library-rental-eda`.
- Current modules are:
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
- Shared event, command, result, and common value contracts belong in `common-events` when they are used by more than one service.

## Architecture Principles

- Use DDD and Hexagonal Architecture as the default design style.
- Domain code should contain business rules and stay independent from Spring, JPA, MongoDB, Kafka, Redis, and web frameworks.
- Application code should orchestrate use cases and depend on domain abstractions.
- Adapters should handle web, Kafka, persistence, Redis, and other infrastructure concerns.
- Do not expose domain entities directly from controllers.
- Do not put persistence annotations on pure domain models unless the existing code already uses that pattern and the task is explicitly scoped to preserve it.
- Keep inbound ports, outbound ports, use cases, domain models, and adapters separated.
- Prefer Java `record` for DTOs, commands, events, and simple immutable message payloads when compatible with the existing code.

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
│   └── model/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── dto/
│   │   └── messaging/
│   │       └── consumer/
│   └── out/
│       ├── messaging/
│       └── persistence/
└── infrastructure/
    ├── messaging/
    ├── security/
    └── common/
```

For `common-events`, keep shared contracts under:

```text
com.example.library.common/
├── event/
└── vo/
```

## EDA Rules

- Distinguish command messages, domain events, and result events by name and purpose.
- Command messages express requested actions.
- Domain events express facts that already happened.
- Result events express command processing outcomes.
- Include correlation identifiers such as `commandId` where asynchronous command tracking is needed.
- Kafka consumers should live in `adapter/in/messaging/consumer`.
- Kafka producers should live in `adapter/out/messaging` and implement outbound event ports from `application/port/out`.
- Persistence adapters should live in `adapter/out/persistence` and implement repository/output ports from `application/port/out`.
- In `bestbook-service`, persistence adapters should use MongoDB `@Document` models and `MongoRepository`, not JPA entities or `JpaRepository`.
- Consumers should deserialize, validate minimally, and delegate to application use cases.
- Business decisions should not live in Kafka consumer classes.

## Infrastructure Rules

- `infrastructure` contains technical support code only: Kafka serializers/deserializers and messaging support, security support, common technical utilities, and other framework helpers.
- `config` contains Spring `@Configuration` classes and bean wiring. Configuration classes may wire infrastructure support, adapters, and framework beans.
- Kafka consumers are not infrastructure; keep them in `adapter/in/messaging/consumer`.
- Kafka producers are not infrastructure; keep them in `adapter/out/messaging`.
- JPA entities, MongoDB documents, persistence mappers, and Spring Data repositories are not infrastructure; keep them in `adapter/out/persistence`.
- Domain and application code must not depend on `infrastructure`.
- Do not put business rules, compensation decisions, use case orchestration, or service-to-service workflow logic in `infrastructure`.

## Validation

- Prefer targeted Gradle checks while developing.
- Use `.\gradlew.bat test` for full verification when practical.
- Use module-level tests when the change is scoped to one service.
