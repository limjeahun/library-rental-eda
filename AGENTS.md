# Library Rental EDA

## Instruction Priority

1. Follow this `AGENTS.md` first.
2. Use `docs/architecture-rule-eda.md` as a reference for DDD, Hexagonal Architecture, DTO separation, ports/adapters, and EDA naming patterns.
3. If `docs/architecture-rule-eda.md` conflicts with this file, this file wins.
4. Preserve the existing project structure and naming unless the user explicitly asks for a refactor.

## Project Stack

- Java application code is written in Java, not Kotlin.
- The project uses Java 21, Gradle Wrapper 8.5, Spring Boot 3.3.7, MariaDB, Redis, and Kafka KRaft.
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

Use normal Spring Boot, Spring Kafka, JPA, Redis, and Gradle patterns already present in the project.

## Service Boundary Rules

- Keep service boundaries strict.
- Service-to-service communication must use Kafka event or command messaging only.
- Do not add `RestTemplate`, `WebClient`, OpenFeign, or other direct HTTP clients for service-to-service communication.
- Cross-service state changes must be modeled as Kafka command/event flows.
- Cross-service reads must not be implemented with direct HTTP calls. Prefer event-maintained local read models or ask for the intended design when unclear.
- Shared event, command, result, and common value contracts belong in `common-events` when they are used by more than one service.

## Architecture Principles

- Use DDD and Hexagonal Architecture as the default design style.
- Domain code should contain business rules and stay independent from Spring, JPA, Kafka, Redis, and web frameworks.
- Application code should orchestrate use cases and depend on domain abstractions.
- Framework adapters should handle web, Kafka, persistence, Redis, and other infrastructure concerns.
- Do not expose domain entities directly from controllers.
- Do not put persistence annotations on pure domain models unless the existing code already uses that pattern and the task is explicitly scoped to preserve it.
- Keep inbound ports, outbound ports, use cases, domain models, and framework adapters separated.
- Prefer Java `record` for DTOs, commands, events, and simple immutable message payloads when compatible with the existing code.

## Current Package Structure

For `book-service`, `member-service`, and `rental-service`, follow the existing structure:

```text
com.example.library.{service}/
├── application/
│   ├── inputport/
│   ├── outputport/
│   └── usecase/
├── config/
├── domain/
│   └── model/
└── framework/
    ├── jpaadapter/
    ├── kafkaadapter/
    └── web/
        └── dto/
```

For `bestbook-service`, preserve its existing structure unless a refactor is requested:

```text
com.example.library.bestbook/
├── config/
├── domain/
│   └── model/
├── framework/
│   ├── jpaadapter/
│   ├── kafkaadapter/
│   └── web/
│       └── dto/
└── service/
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
- Kafka producers and consumers should live in framework Kafka adapters.
- Consumers should deserialize, validate minimally, and delegate to application use cases.
- Business decisions should not live in Kafka consumer classes.

## Validation

- Prefer targeted Gradle checks while developing.
- Use `.\gradlew.bat test` for full verification when practical.
- Use module-level tests when the change is scoped to one service.
