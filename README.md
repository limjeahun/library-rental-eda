# 사내 도서관 도서 대여 시스템

Java 21, Spring Boot 3.3.7, Gradle Kotlin DSL 기반의 멀티 모듈 마이크로서비스 예제입니다. 회원, 도서, 대여, 베스트도서 서비스를 DDD + 헥사고널 아키텍처로 분리하고, 서비스 간 직접 HTTP 호출 없이 Kafka 이벤트와 커맨드로만 연동합니다.

## 모듈 구성

- `common-events`: Kafka 메시지 계약 전용 모듈
- `rental-service`: 대여카드, 대여, 반납, 연체, 연체해제, SAGA 보상 트랜잭션
- `book-service`: 도서 등록/조회, 대여 시 이용불가, 반납 시 이용가능, `rental_result` 응답
- `member-service`: 회원 등록/조회, 포인트 적립/사용, 연체해제 결과 응답, 보상 포인트 차감
- `bestbook-service`: 대여 이벤트 기반 베스트도서 카운트 증가

## 기술 스택

- Java 21 toolchain
- Gradle Wrapper 8.5, Kotlin DSL Gradle
- Spring Boot 3.3.7
- Spring Web, Validation, Security permitAll
- Spring Data JPA, Hibernate, MariaDB JDBC
- QueryDSL JPA 5.0.0 `jakarta`
- Spring Kafka
- Spring Data Redis
- Docker Compose: MariaDB 11.4, Redis 7.4-alpine, Apache Kafka 3.9.0 KRaft

## 실행 전 준비

Docker와 Java/Gradle 실행 환경이 필요합니다. 로컬 Java가 21이 아니어도 Gradle toolchain resolver가 Java 21을 내려받도록 설정되어 있습니다.

```bash
docker compose up -d
```

애플리케이션은 컨테이너로 실행하지 않고 로컬 Gradle로 각각 실행합니다.

```bash
./gradlew :member-service:bootRun
./gradlew :book-service:bootRun
./gradlew :rental-service:bootRun
./gradlew :bestbook-service:bootRun
```

포트:

- RentalMS: `8080`
- BookMS: `8081`
- MemberMS: `8082`
- BestBookMS: `8084`

## Kafka 토픽

- `rental_rent`: RentalMS가 도서 대여 이벤트 `ItemRented` 발행
- `rental_return`: RentalMS가 도서 반납 이벤트 `ItemReturned` 발행
- `overdue_clear`: RentalMS가 연체정지 해제 이벤트 `OverdueCleared` 발행
- `rental_result`: BookMS/MemberMS가 처리 결과 `EventResult` 응답
- `point_use`: RentalMS가 대여/반납 실패 보상용 포인트 차감 커맨드 발행

## 정상 EDA 테스트

회원 등록:

```bash
curl -X POST http://localhost:8082/api/Member/ \
  -H "Content-Type: application/json" \
  -d '{"id":"jenny","name":"제니","passWord":"1111","email":"scant10@gmail.com"}'
```

도서 등록:

```bash
curl -X POST http://localhost:8081/api/book \
  -H "Content-Type: application/json" \
  -d '{"author":"한정헌","classfication":"LITERATURE","description":"고전 소설","isbn":"1232141214","location":"JEONGJA","publicationDate":"2023-02-11","source":"SUPPLY","title":"누구를 위하여 종을 울리나?"}'
```

대여카드 생성:

```bash
curl -X POST http://localhost:8080/api/RentalCard/ \
  -H "Content-Type: application/json" \
  -d '{"UserId":"jenny","UserNm":"제니"}'
```

도서 대여:

```bash
curl -X POST http://localhost:8080/api/RentalCard/rent \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"itemTitle":"누구를 위하여 종을 울리나?","userId":"jenny","userNm":"제니"}'
```

확인:

```bash
curl http://localhost:8081/api/book/1
curl http://localhost:8082/api/Member/1
curl http://localhost:8084/api/books
```

기대 결과는 도서 상태 `UNAVAILABLE`, 회원 포인트 `+10`, 베스트도서 `rentCount` 증가입니다.

도서 반납:

```bash
curl -X POST http://localhost:8080/api/RentalCard/return \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"itemTitle":"누구를 위하여 종을 울리나?","userId":"jenny","userNm":"제니"}'
```

다시 확인하면 도서 상태는 `AVAILABLE`, 회원 포인트는 `+20`이 됩니다.

연체와 정지해제 흐름은 다음 순서로 수동 검증합니다.

```bash
curl -X POST http://localhost:8080/api/RentalCard/rent \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"itemTitle":"누구를 위하여 종을 울리나?","userId":"jenny","userNm":"제니"}'

curl -X POST http://localhost:8080/api/RentalCard/overdue \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"itemTitle":"누구를 위하여 종을 울리나?","userId":"jenny","userNm":"제니"}'

curl -X POST http://localhost:8080/api/RentalCard/return \
  -H "Content-Type: application/json" \
  -d '{"itemId":1,"itemTitle":"누구를 위하여 종을 울리나?","userId":"jenny","userNm":"제니"}'

curl -X POST http://localhost:8080/api/RentalCard/clearoverdue \
  -H "Content-Type: application/json" \
  -d '{"userId":"jenny","userNm":"제니","point":0}'
```

실제 연체료는 반납 예정일보다 늦은 날짜로 반납될 때 발생합니다. API는 현재 날짜 반납을 사용하므로 장기 연체료 검증은 도메인 테스트 또는 DB 날짜 조정으로 확인합니다.

## SAGA 실패/보상 테스트

BookMS 대여 실패 보상:

```bash
./gradlew :book-service:bootRun --args='--app.failure.force-rent-fail=true'
```

이 상태에서 대여하면 BookMS가 `EventResult{RENT,false}`를 `rental_result`로 발행하고, RentalMS가 `cancleRentItem`을 실행한 뒤 `point_use` 커맨드를 발행합니다. MemberMS는 `point_use`를 소비해 적립 취소 포인트를 차감합니다.

BookMS 반납 실패 보상:

```bash
./gradlew :book-service:bootRun --args='--app.failure.force-return-fail=true'
```

반납하면 BookMS가 `EventResult{RETURN,false}`를 발행하고, RentalMS가 `cancleReturnItem`을 실행한 뒤 `point_use` 커맨드를 발행합니다.

MemberMS 연체해제 실패 보상:

```bash
./gradlew :member-service:bootRun --args='--app.failure.force-overdue-clear-fail=true'
```

연체해제 시 MemberMS가 `EventResult{OVERDUE,false}`를 발행하고, RentalMS가 `cancleMakeAvailableRental`을 실행합니다. 원자료 흐름처럼 이 경우 별도 `point_use` 커맨드는 발행하지 않습니다.

## 현재 구현 범위

- DDD 도메인 모델
- 헥사고널 아키텍처: usecase/inputport/outputport/framework adapter 분리
- Kafka 기반 EDA
- SAGA Choreography
- `EventResult` 기반 성공/실패 응답 메시지
- RentalMS 보상 트랜잭션
- Redis 기반 Idempotent Consumer
- MariaDB + Spring Data JPA + Hibernate
- QueryDSL JPA 실제 조회 구현
- Spring Security 개발용 `permitAll`, CSRF disabled
- REST API와 도메인 단위 테스트

## 향후 실전 보강점

이번 구현에서는 다음 항목을 코드로 구현하지 않았습니다.

1. 현재 구현은 DB commit과 Kafka publish의 원자성을 완전히 보장하지 않습니다.
2. 실무에서는 Outbox Pattern 또는 Outbox + CDC + Debezium을 적용해야 합니다.
3. 현재 구현은 Redis 기반 Idempotent Consumer로 중복 소비를 방어합니다.
4. DLQ는 아직 없으므로 보상 트랜잭션 자체가 실패한 메시지를 별도 토픽에 격리하지 못합니다.
5. Kafka Retry/Backoff 커스텀 정책은 아직 없으므로 재시도 전략은 향후 보강해야 합니다.
6. 분산 추적은 아직 없으므로 `correlationId`는 메시지에 포함하지만 Zipkin, Tempo, OpenTelemetry 연동은 하지 않습니다.
7. 현재 SAGA는 Choreography 방식이며, Orchestration 방식으로 전환하려면 `RentalSagaOrchestrator` 같은 중앙 오케스트레이터가 필요합니다.

즉 Outbox Pattern, DLQ, Distributed Tracing, Kafka Retry/Backoff 커스텀 정책, SAGA Orchestration은 이번 범위에서 구현하지 않았습니다.

## 빌드와 테스트

```bash
./gradlew clean build
```

통합 테스트는 Kafka, Redis, MariaDB 인프라가 필요하므로 위 수동 시나리오로 검증합니다. 단위 테스트는 각 서비스 도메인 규칙을 검증합니다.
