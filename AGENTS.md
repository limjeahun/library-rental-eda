# Library Rental EDA

- Java application code is written in Java, not Kotlin.
- The project uses Java 21, Gradle Wrapper 8.5, Spring Boot 3.3.7, MariaDB, Redis, and Kafka KRaft.
- Keep service boundaries strict: no direct HTTP calls between services.
- Service-to-service communication is Kafka event or command messaging only.
- This implementation intentionally excludes Outbox, DLQ, distributed tracing, custom Kafka retry/backoff, and SAGA orchestration code.
