# [Architecture Rule] Library Rental DDD Hexagonal + Kafka EDA-SAGA + DTO 분리 통합 아키텍처 규칙

> 적용 프로젝트: `library-rental-eda-saga`  
> 적용 도메인: 사내 도서관 도서 대여 시스템  
> 적용 서비스: `rental-service`, `book-service`, `member-service`, `bestbook-service`, `common-events`  
> 핵심 패턴: DDD + Hexagonal Architecture + Kafka EDA + SAGA Choreography + 계층별 DTO 분리

---

## 0. 문서 목적과 적용 범위

이 문서는 사내 도서관 도서 대여 시스템을 다음 구조로 구현하기 위한 아키텍처 규칙이다.

- 대여, 도서, 회원, 베스트도서 4개 마이크로서비스
- DDD 도메인 모델 중심 설계
- 헥사고널 아키텍처
- Kafka 기반 EDA
- SAGA Choreography 기반 보상 트랜잭션
- Web DTO, Application DTO, Event DTO 분리
- MariaDB/JPA/QueryDSL, Redis, Kafka KRaft 기반 현대화 구현

기존 일반 가이드의 Merchant/Payment/Admin 예시는 이 프로젝트에 맞지 않으므로 사용하지 않는다. 이 문서의 모든 예시는 사내 도서관 대여 도메인을 기준으로 작성한다.

---

## 1. 실제 구현 범위와 제외 범위

### 1-1. 이번 프로젝트에서 실제 구현한다

| 영역 | 구현 내용 |
|---|---|
| DDD | `RentalCard`, `Book`, `Member`, `BestBook` 중심 도메인 모델 |
| Hexagonal | 도메인 헥사곤, 애플리케이션 헥사곤, 프레임워크 헥사곤 분리 |
| EDA | `rental_rent`, `rental_return`, `overdue_clear` 도메인 이벤트 발행/소비 |
| SAGA Choreography | `rental_result` 결과 이벤트 기반 보상 트랜잭션 |
| Command Message | `point_use` 포인트 차감 커맨드 |
| Idempotency | Redis 기반 Consumer 멱등 처리 |
| Persistence | MariaDB + Spring Data JPA + QueryDSL |
| Messaging | Spring Kafka + Kafka KRaft 단일 노드 |
| API | 각 서비스 REST API |
| 테스트 | 도메인 단위 테스트, README 기반 통합 시나리오 |

### 1-2. 이번 프로젝트에서 구현하지 않는다

아래 항목은 실전 운영에서 필요하지만, 이번 교육 구현 범위에서는 코드로 구현하지 않는다.

| 항목 | 이번 범위 처리 |
|---|---|
| Outbox Pattern | 실제 구현하지 않음. README에 한계와 향후 과제로 명시 |
| DLQ / DLT | 실제 구현하지 않음. README에 향후 과제로 명시 |
| Distributed Tracing | correlationId 필드만 둠. OpenTelemetry/Zipkin/Tempo 연동은 하지 않음 |
| Kafka Retry/Backoff 커스텀 정책 | 실제 구현하지 않음. README에 향후 과제로 명시 |
| SAGA Orchestration | 구현하지 않음. 현재 방식은 Choreography임을 명시 |

### 1-3. 구현하지 않은 것을 구현했다고 말하지 않는다

문서, README, 최종 보고에서 다음과 같이 명확히 표현한다.

- “현재 구현은 DB commit과 Kafka publish의 원자성을 완전히 보장하지 않는다.”
- “실무에서는 Outbox Pattern 또는 Outbox + CDC + Debezium을 적용해야 한다.”
- “현재 구현은 Redis 기반 Idempotent Consumer로 중복 소비를 방어한다.”
- “DLQ, Retry/Backoff, 분산 추적은 향후 실전 보강점이다.”
- “현재 SAGA는 Choreography 방식이며 Orchestration은 구현하지 않았다.”

---

## 2. Bounded Context와 서비스 책임

| Bounded Context | 서비스 | 도메인 유형 | 핵심 책임 | 저장소 | Kafka 역할 |
|---|---|---|---|---|---|
| 대여 | `rental-service` | 핵심 Core | 대여카드, 대여, 반납, 연체, 정지해제, 보상 트랜잭션 | MariaDB | 이벤트 생산, 결과 소비, 보상 수행 |
| 도서 | `book-service` | 일반 Generic | 도서 등록, 도서 상태 AVAILABLE/UNAVAILABLE 변경 | MariaDB | 대여/반납 이벤트 소비, 결과 이벤트 생산 |
| 회원 | `member-service` | 일반 Generic | 회원 등록, 포인트 적립/사용 | MariaDB | 대여/반납/정지해제 이벤트 소비, 일부 결과 이벤트 생산 |
| 베스트도서 | `bestbook-service` | 지원 Supporting | 대여 이벤트 기반 베스트도서 카운트 집계 | MariaDB | 대여 이벤트 단방향 소비 |
| 공통 메시지 | `common-events` | 공유 계약 | 서비스 간 이벤트/커맨드 메시지 계약 | 없음 | 메시지 클래스 제공 |

### 2-1. 서비스 간 데이터 소유권

- `rental-service`는 도서 DB나 회원 DB를 직접 조회하지 않는다.
- `book-service`는 대여 DB나 회원 DB를 직접 조회하지 않는다.
- `member-service`는 대여 DB나 도서 DB를 직접 조회하지 않는다.
- `bestbook-service`는 `rental_rent` 이벤트를 통해 필요한 도서 정보를 자기 저장소에 반영한다.
- 서비스 간 상태 변경은 Kafka 이벤트/커맨드로 처리한다.
- 조회는 각 서비스의 REST API로 동기 처리할 수 있다.

---

## 3. Hexagonal Architecture 기본 규칙

교육 자료의 헥사고널 구조는 다음 세 영역으로 나뉜다.

| 헥사곤 | 구성 요소 | 책임 |
|---|---|---|
| 도메인 헥사곤 | Aggregate, Entity, Value Object, Enum, Domain Event | 비즈니스 개념과 규칙 구현 |
| 애플리케이션 헥사곤 | Usecase Interface, InputPort, OutputPort | 트랜잭션, 흐름 제어, 유스케이스 구현 |
| 프레임워크 헥사곤 | Web Adapter, JPA Adapter, Kafka Adapter, Config | 외부 입출력, 저장소, 메시징, API 제공 |

### 3-1. 의존성 방향

```text
Framework Adapter  →  Application  →  Domain
        │                  │             ▲
        │                  └─ Port ──────┘
        └─ implements OutputPort
```

원칙:

- Domain은 Application, Framework를 모른다.
- Application은 Domain을 알고, OutputPort 인터페이스를 정의한다.
- Framework Adapter는 Application의 Port를 구현한다.
- Controller는 Usecase만 호출한다.
- Kafka Producer는 EventOuputPort를 구현한다.
- JPA Adapter는 RentalCardOuputPort, BookOutPort, MemberOutPutPort 등을 구현한다.

### 3-2. 교육형 JPA 매핑 타협 규칙

이 프로젝트는 교육 자료의 현실적 타협을 따른다.

도메인 모델을 완전히 순수 POJO로 유지하고 별도 JPA Entity를 두는 방식은 이상적이지만, 교육 실습에서는 보일러플레이트가 커진다. 따라서 아래 조건을 지키는 경우 도메인 모델에 JPA 매핑 어노테이션을 둘 수 있다.

허용:

- `@Entity`
- `@Embeddable`
- `@Embedded`
- `@ElementCollection`
- `@Enumerated`
- `@Id`
- `@GeneratedValue`

금지:

- Domain에서 `JpaRepository` 직접 참조
- Domain에서 `EntityManager` 직접 참조
- Domain에서 KafkaTemplate 직접 참조
- Domain에서 RedisTemplate 직접 참조
- Domain에서 Spring Service/Component 역할 수행
- Domain에서 HTTP, Kafka, DB 세부 설정 참조

정리:

```text
JPA 어노테이션은 교육형 구현 편의를 위해 허용한다.
그러나 Repository, Kafka, Redis, Web 같은 기술 행위는 반드시 Framework Adapter에 둔다.
```

실무 고도화 단계에서는 Domain Entity와 JPA Entity를 분리하고 Mapper를 둔다.

---

## 4. 프로젝트 및 패키지 구조

### 4-1. 멀티 모듈 구조

```text
library-rental-eda-saga/
├── common-events
├── rental-service
├── book-service
├── member-service
└── bestbook-service
```

### 4-2. 공통 패키지 규칙

각 서비스는 아래 구조를 기본으로 한다.

```text
com.example.library.<service>/
├── <Service>Application.java
├── application
│   ├── inputport
│   ├── outputport
│   └── usecase
├── config
├── domain
│   └── model
└── framework
    ├── jpaadapter
    ├── kafkaadapter
    └── web
```

### 4-3. 패키지별 책임

| 패키지 | 책임 | 포함 가능 | 포함 금지 |
|---|---|---|---|
| `domain.model` | 도메인 개념과 규칙 | Aggregate, Entity, VO, Enum, 도메인 팩토리 | Repository 호출, Kafka 발행, REST DTO 반환 |
| `application.usecase` | 유스케이스 계약 | `RentItemUsecase`, `CompensationUsecase` | 구현 로직 |
| `application.inputport` | 유스케이스 구현 | `@Service`, `@Transactional`, 도메인 호출, Port 호출 | Controller, KafkaTemplate, JpaRepository 직접 사용 |
| `application.outputport` | 외부 의존 추상화 | Repository Port, Event Port | 구현체 |
| `framework.web` | REST API | Controller, Request/Response DTO, Validation | 비즈니스 규칙 |
| `framework.jpaadapter` | DB Adapter | JpaRepository, QueryDSL, Port 구현체 | Controller, Kafka Consumer |
| `framework.kafkaadapter` | Kafka Adapter | Producer, Consumer, 메시지 변환, 멱등 처리 | 도메인 규칙 직접 구현 |
| `config` | 설정 | KafkaConfig, RedisConfig, SecurityConfig, ObjectMapperConfig | 유스케이스 로직 |

---

## 5. 도메인 모델 규칙

### 5-1. Aggregate Root는 상태 변경의 유일한 진입점이다

Aggregate 내부 상태는 Aggregate Root 메서드를 통해서만 변경한다.

#### rental-service

Aggregate Root: `RentalCard`

필수 도메인 메서드:

```text
createRentalCard(IDName creator)
rentItem(Item item)
returnItem(Item item, LocalDate returnDate)
overdueItem(Item item)
makeAvailableRental(long point)
cancleRentItem(Item item)
cancleReturnItem(Item item, long point)
cancleMakeAvailableRental(long point)
```

필수 규칙:

- 최초 대여카드는 `RENT_AVAILABLE`
- 최초 연체료는 0
- 대여 기간은 14일
- 대여 중인 도서는 최대 5권
- `RENT_UNAVAILABLE`이면 대여 불가
- 1권이라도 연체되면 `RENT_UNAVAILABLE`
- 반납 시 반납예정일보다 늦으면 `지연일수 * 10` 포인트 연체료 부과
- 모든 도서가 반납되어야 정지해제 가능
- 정지해제 입력 포인트는 현재 연체료와 같아야 함
- 정지해제 성공 시 연체료가 0이면 `RENT_AVAILABLE`

#### book-service

Aggregate Root: `Book`

필수 도메인 메서드:

```text
enterBook(...)
makeAvailable()
makeUnAvailable()
```

필수 규칙:

- 신규 도서는 `ENTERED`
- 사서 처리 후 `AVAILABLE`
- 대여 이벤트 수신 시 `UNAVAILABLE`
- 반납 이벤트 수신 시 `AVAILABLE`

주의:

- 원자료에는 `makeUnavailabe()`가 `AVAILABLE`로 세팅되는 오타가 있다.
- 이번 구현에서는 실제 의미대로 `UNAVAILABLE`로 세팅한다.
- 원자료 추적을 위해 alias 메서드를 둘 수 있지만, 최종 상태는 정확해야 한다.

#### member-service

Aggregate Root: `Member`

필수 도메인 메서드:

```text
registerMember(IDName idName, PassWord pwd, Email email)
savePoint(long point)
usePoint(long point)
addAuthority(Authority authority)
```

필수 규칙:

- 신규 회원 기본 권한은 `USER`
- 최초 포인트는 0
- 대여 이벤트 수신 시 포인트 적립
- 반납 이벤트 수신 시 포인트 적립
- 정지해제 이벤트 수신 시 포인트 사용
- 보유 포인트보다 많이 사용하면 예외

#### bestbook-service

Aggregate Root: `BestBook`

필수 도메인 메서드:

```text
registerBestBook(Item item)
increseBestBookCount()
```

필수 규칙:

- 최초 등록 시 `rentCount = 1`
- 같은 도서가 다시 대여되면 `rentCount + 1`
- 대여취소 SAGA가 발생해도 원자료 결정에 따라 베스트도서는 보상하지 않음

### 5-2. Domain Event 생성 책임

도메인의 상태 변화가 발생하면 도메인 이벤트를 생성할 수 있어야 한다.

`RentalCard`는 다음 정적 팩토리를 가져야 한다.

```text
createItemRentedEvent(IDName idName, Item item, long point)
createItemReturnEvent(IDName idName, Item item, long point)
createOverdueCleardEvent(IDName idName, long point)
```

이벤트 발행 자체는 Domain이 하지 않는다.

```text
Domain: 이벤트 객체 생성 가능
Application: 이벤트 발행 Port 호출
Framework: Kafka로 실제 발행
```

---

## 6. 원자료 식별자 유지 규칙

교육 자료 추적성을 위해 아래 오탈자 식별자는 반드시 유지한다.

| 원자료 식별자 | 의미 |
|---|---|
| `RentalCardOuputPort` | 대여카드 저장소 OutputPort |
| `EventOuputPort` | 이벤트 발행 OutputPort |
| `ReturnItemUsercase` | 반납 유스케이스 |
| `RentalResultOuputDTO` | 연체해제 결과 DTO |
| `RetrunItemOupputDTO` | 반납목록 조회 DTO |
| `occurRetunEvent` | 반납 이벤트 발행 메서드 |
| `occurOverdueClearEvent` | 정지해제 이벤트 발행 메서드 |
| `createOverdueCleardEvent` | 정지해제 이벤트 생성 메서드 |
| `cancleRentItem` | 대여취소 보상 메서드 |
| `cancleReturnItem` | 반납취소 보상 메서드 |
| `cancleMakeAvailableRental` | 정지해제취소 보상 메서드 |
| `increseBestBookCount` | 베스트도서 카운트 증가 메서드 |

컴파일 안정성을 위해 올바른 철자의 alias를 추가할 수 있다.

예:

```java
public RentalCard cancelRentItem(Item item) {
    return cancleRentItem(item);
}
```

그러나 원자료 식별자는 반드시 존재해야 한다.

---

## 7. Application Layer 규칙

### 7-1. Usecase는 “소리치는 아키텍처”를 따른다

클래스명과 인터페이스명만 보고 사용 사례를 알 수 있어야 한다.

예:

```text
CreateRentalCardUsecase
RentItemUsecase
ReturnItemUsercase
OverdueItemUsercase
ClearOverdueItemUsecase
InquiryUsecase
CompensationUsecase
```

### 7-2. InputPort의 책임

InputPort는 유스케이스 구현체다.

해야 할 일:

1. OutputPort로 Aggregate 로딩
2. 없으면 필요한 경우 Aggregate 생성
3. Domain 메서드 호출
4. 변경된 Aggregate 저장
5. 필요한 경우 Domain Event 생성
6. EventOuputPort로 이벤트 발행 요청
7. DTO/Result 반환

하지 말아야 할 일:

- 도메인 규칙 직접 구현
- JPA Repository 직접 호출
- KafkaTemplate 직접 사용
- RedisTemplate 직접 사용
- Controller Request/Response 직접 생성

### 7-3. OutputPort는 기술이 아니라 비즈니스 필요로 정의한다

나쁜 예:

```java
interface RentalCardJpaRepositoryPort {
    RentalCard findBySql(...);
}
```

좋은 예:

```java
public interface RentalCardOuputPort {
    Optional<RentalCard> loadRentalCard(String userId);
    RentalCard save(RentalCard rentalCard);
}
```

Event Port도 동일하다.

```java
public interface EventOuputPort {
    void occurRentalEvent(ItemRented itemRented);
    void occurRetunEvent(ItemReturned itemReturned);
    void occurOverdueClearEvent(OverdueCleared overdueCleared);
    void occurPointUseCommand(PointUseCommand pointUseCommand);
}
```

Application은 Kafka를 모른다. “이벤트 발생”이라는 비즈니스 행위만 안다.

---

## 8. DTO 분리 규칙

### 8-1. DTO 종류

| DTO 종류 | 위치 | 예시 | 역할 |
|---|---|---|---|
| API Request DTO | `framework.web` | `UserInputDTO`, `UserItemInputDTO`, `BookInfoDTO` | 외부 요청 수신 |
| API Response DTO | `framework.web` | `RentalCardOutputDTO`, `BookOutPutDTO`, `MemberOutPutDTO` | 외부 응답 반환 |
| Application Command/Query/Result | `application.dto` 권장 | `RentItemCommand`, `RentalCardResult` | Usecase 입출력 |
| Event DTO | `common-events` | `ItemRented`, `EventResult`, `PointUseCommand` | Kafka 메시지 계약 |
| Persistence DTO/JPA Entity | `framework.jpaadapter` 또는 교육형 domain | `RentalCard`, `Book` | DB 매핑 |

### 8-2. 교육 자료 호환 DTO 규칙

원자료에서는 `UserInputDTO`, `RentalCardOutputDTO`가 `framework.web`에 위치하면서 유스케이스 입출력으로도 사용된다.

이번 구현에서는 아래 둘 중 하나를 선택할 수 있다.

#### 옵션 A. 교육 자료 호환 모드

- 원자료 DTO명을 그대로 사용
- DTO는 `framework.web`에 위치
- Usecase가 해당 DTO를 직접 받을 수 있음
- 학습 추적성과 원자료 대응이 쉬움

#### 옵션 B. 실무 분리 강화 모드

- Web Request/Response와 Application Command/Result를 분리
- Controller에서 `request.toCommand()` 호출
- Usecase는 Command/Query/Result만 사용
- Response는 `Response.from(result)`로 변환

권장:

- 교육 실습과 Codex 구현은 옵션 A를 기본으로 하되, 복잡도가 증가하면 옵션 B로 전환한다.

### 8-3. 변환 메서드 명명 규칙

| 변환 방향 | 메서드명 | 위치 |
|---|---|---|
| Request → Command | `toCommand()` | API Request DTO |
| Request → Query | `toQuery()` | API Request DTO |
| Domain → Result | `from(Domain)` | Application Result |
| Result → Response | `from(Result)` | API Response DTO |
| Domain → DTO | `mapToDTO(Domain)` | 원자료 호환 DTO |
| Event DTO → Domain 값 | 명시 생성자 또는 Mapper | Kafka Adapter |

원자료 호환을 위해 `mapToDTO()` 명명은 허용한다.

예:

```java
public static RentalCardOutputDTO mapToDTO(RentalCard rentalCard) {
    ...
}
```

### 8-4. record 사용 규칙

Java 21에서는 DTO에 `record`를 권장한다. 단, 다음 예외가 있다.

| 대상 | 권장 형태 | 이유 |
|---|---|---|
| API Request/Response DTO | `record` 권장 | 불변성, 간결성 |
| Application Command/Query/Result | `record` 권장 | 불변성, 명확한 값 전달 |
| Kafka Event/Command DTO | class 허용/권장 | Jackson 역직렬화 기본 생성자/setter 필요 |
| JPA Entity/Embeddable | class 필수 | JPA 기본 생성자 필요 |
| 원자료 식별자 DTO | class 허용 | 원자료와 대응성 유지 |

금지:

- Domain Entity가 `toResponse()`를 가지는 것
- Controller가 Domain Entity를 직접 반환하는 것
- Event DTO가 API Response로 직접 반환되는 것
- API DTO에 도메인 비즈니스 규칙을 넣는 것

---

## 9. EDA 설계 규칙

### 9-1. 이벤트 설계 3원칙

이 프로젝트의 이벤트 설계는 아래 원칙을 따른다.

1. 스트림당 이벤트 정의는 하나만 사용
2. 이벤트는 하나의 목적만 갖는다
3. 이벤트 크기 최소화

### 9-2. 메시지 종류 구분

| 메시지 종류 | 예시 | 의미 | 방향 |
|---|---|---|---|
| Domain Event | `ItemRented` | 도서가 대여되었다는 과거 사실 | RentalMS → Kafka → 타 서비스 |
| Domain Event | `ItemReturned` | 도서가 반납되었다는 과거 사실 | RentalMS → Kafka → 타 서비스 |
| Domain Event | `OverdueCleared` | 대여정지가 해제되었다는 과거 사실 | RentalMS → Kafka → MemberMS |
| Result Event | `EventResult` | 처리 성공/실패 응답 | BookMS/MemberMS → RentalMS |
| Command Message | `PointUseCommand` | 포인트를 차감하라는 요청 | RentalMS → MemberMS |

중요:

- `ItemRented`는 “대여하라”가 아니라 “대여되었다”이다.
- `PointUseCommand`는 이벤트가 아니라 명령이다.
- `EventResult`는 도메인 이벤트가 아니라 SAGA 응답 메시지다.

### 9-3. Kafka 토픽 규칙

| 토픽 | 메시지 타입 | 생산자 | 소비자 | 목적 |
|---|---|---|---|---|
| `rental_rent` | `ItemRented` | RentalMS | BookMS, MemberMS, BestBookMS | 도서대여됨 이벤트 |
| `rental_return` | `ItemReturned` | RentalMS | BookMS, MemberMS | 도서반납됨 이벤트 |
| `overdue_clear` | `OverdueCleared` | RentalMS | MemberMS | 대여정지해제됨 이벤트 |
| `rental_result` | `EventResult` | BookMS, MemberMS | RentalMS | SAGA 성공/실패 결과 |
| `point_use` | `PointUseCommand` | RentalMS | MemberMS | 보상 시 포인트 차감 |

### 9-4. Consumer Group 규칙

| 서비스 | groupId |
|---|---|
| `book-service` | `book` |
| `member-service` | `member` |
| `bestbook-service` | `bestbook` |
| `rental-service` | `rental` |

같은 이벤트를 여러 서비스가 각각 받아야 하므로 Consumer Group을 서비스별로 분리한다.

### 9-5. 이벤트 흐름

#### 대여 이벤트

```text
RentalMS.rentItem()
  → RentalCard.rentItem()
  → RentalCard.createItemRentedEvent(...)
  → EventOuputPort.occurRentalEvent(...)
  → Kafka(rental_rent)
  → BookMS: 도서 UNAVAILABLE
  → MemberMS: 포인트 +10
  → BestBookMS: 카운트 증가
```

#### 반납 이벤트

```text
RentalMS.returnItem()
  → RentalCard.returnItem(...)
  → RentalCard.createItemReturnEvent(...)
  → EventOuputPort.occurRetunEvent(...)
  → Kafka(rental_return)
  → BookMS: 도서 AVAILABLE
  → MemberMS: 포인트 +10
```

#### 정지해제 이벤트

```text
RentalMS.clearOverdue()
  → RentalCard.makeAvailableRental(...)
  → RentalCard.createOverdueCleardEvent(...)
  → EventOuputPort.occurOverdueClearEvent(...)
  → Kafka(overdue_clear)
  → MemberMS: 포인트 사용
```

### 9-6. Kafka Adapter 책임

Kafka Consumer는 다음만 담당한다.

1. 메시지 수신
2. JSON 역직렬화
3. eventId 기반 멱등 확인
4. Application Usecase 호출
5. 필요한 경우 Result Event 발행

Kafka Consumer가 직접 도메인 상태를 변경하지 않는다.

---

## 10. SAGA Choreography 규칙

### 10-1. 현재 프로젝트는 Choreography 방식이다

현재 구현은 중앙 오케스트레이터가 없다.

- BookMS는 자기 처리를 한 뒤 결과를 발행한다.
- MemberMS는 정지해제 처리 결과를 발행한다.
- RentalMS는 결과를 소비하고 실패일 때 보상한다.

`RentalSagaOrchestrator` 같은 중앙 조율자는 만들지 않는다.

### 10-2. EventResult 메시지 규칙

`EventResult`는 SAGA 성공/실패 응답의 통합 포맷이다.

필수 필드:

```text
eventId
correlationId
occurredAt
eventType: RENT | RETURN | OVERDUE
successed: boolean
idName
item
point
reason
```

규칙:

- `successed=true`: RentalMS는 아무 보상도 하지 않는다.
- `successed=false`: RentalMS는 `eventType`에 따라 보상한다.
- 실패 이유는 `reason`에 기록한다.
- `eventId`는 멱등 처리 키로 사용한다.
- `correlationId`는 원 요청과 결과를 연결한다.

### 10-3. SAGA 응답 발행 책임

| 서비스 | 이벤트 | 응답 여부 |
|---|---|---|
| BookMS | `ItemRented` 소비 | 항상 `EventResult{RENT, true/false}` 발행 |
| BookMS | `ItemReturned` 소비 | 항상 `EventResult{RETURN, true/false}` 발행 |
| MemberMS | `ItemRented` 소비 | 응답 발행하지 않음 |
| MemberMS | `ItemReturned` 소비 | 응답 발행하지 않음 |
| MemberMS | `OverdueCleared` 소비 | 항상 `EventResult{OVERDUE, true/false}` 발행 |
| BestBookMS | `ItemRented` 소비 | 응답 발행하지 않음 |

### 10-4. RentalEventConsumers 보상 분기

RentalMS는 `rental_result`를 구독한다.

```text
if EventResult.successed == false:
  switch EventResult.eventType:
    RENT    → cancleRentItem + point_use 발행
    RETURN  → cancleReturnItem + point_use 발행
    OVERDUE → cancleMakeAvailableRental, point_use 발행 안 함
```

보상 로그는 원자료 문구를 유지한다.

```text
대여취소 보상트랜젝션 실행
반납취소 보상트랜젝션 실행
연체해제처리취소 보상트랜젝션 실행
```

### 10-5. 보상 트랜잭션 규칙

#### RENT 실패

```text
BookMS가 도서 UNAVAILABLE 처리 실패
→ EventResult{RENT,false}
→ RentalMS.cancleRentItem
→ point_use 발행
→ MemberMS 포인트 차감
```

#### RETURN 실패

```text
BookMS가 도서 AVAILABLE 처리 실패
→ EventResult{RETURN,false}
→ RentalMS.cancleReturnItem
→ point_use 발행
→ MemberMS 포인트 차감
```

#### OVERDUE 실패

```text
MemberMS가 포인트 사용 실패
→ EventResult{OVERDUE,false}
→ RentalMS.cancleMakeAvailableRental
→ point_use 발행하지 않음
```

### 10-6. BestBook 보상 제외 규칙

베스트도서는 대여 이벤트를 단방향으로 소비한다.

- 대여취소 SAGA가 발생해도 베스트도서 카운트는 되돌리지 않는다.
- BestBookMS는 `rental_result`를 발행하지 않는다.
- BestBookMS는 SAGA 보상 참여자가 아니다.

---

## 11. Redis 기반 Consumer 멱등성 규칙

Kafka는 장애, 재시도, 리밸런싱 상황에서 같은 메시지를 다시 전달할 수 있다.
따라서 모든 Consumer는 eventId 기준 멱등 처리를 해야 한다.

### 11-1. 멱등 키 규칙

| 서비스 | Redis Key |
|---|---|
| RentalMS | `processed:rental:<eventId>` |
| BookMS | `processed:book:<eventId>` |
| MemberMS | `processed:member:<eventId>` |
| BestBookMS | `processed:bestbook:<eventId>` |

TTL:

```text
7일
```

### 11-2. 처리 순서

```text
1. Kafka 메시지 수신
2. eventId 추출
3. Redis에 processed key 존재 여부 확인
4. 이미 존재하면 skip
5. 없으면 처리 시작
6. 도메인 처리 성공 후 processed key 저장
```

주의:

- 처리 전에 key를 저장하면 처리 실패 메시지가 유실될 수 있다.
- 처리 후 key 저장을 기본으로 한다.
- 완전한 exactly-once는 아니다. 실전에서는 Inbox/Outbox를 함께 고려한다.

---

## 12. 트랜잭션과 이벤트 발행 규칙

### 12-1. InputPort는 트랜잭션 경계다

모든 상태 변경 InputPort는 다음을 적용한다.

```java
@Service
@Transactional
```

### 12-2. 도메인 변경 후 반드시 저장한다

특히 `rental-service`는 원자료보다 저장을 더 명확히 한다.

- `rentItem`: 새 카드 생성 또는 기존 카드 변경 후 save
- `returnItem`: 반납 후 save
- `overdueItem`: 연체 처리 후 save
- `clearOverdue`: 정지해제 후 save
- `cancleRentItem`: 보상 후 save
- `cancleReturnItem`: 보상 후 save
- `cancleMakeAvailableRental`: 보상 후 save

### 12-3. 현재 프로젝트의 원자성 한계

현재 구현은 다음을 완전히 보장하지 않는다.

```text
DB commit 성공 + Kafka publish 성공의 원자성
```

따라서 README에 반드시 한계를 적는다.

실무 전환 시 권장:

```text
Outbox Pattern
Outbox + CDC + Debezium
Kafka Retry/Backoff
DLQ/DLT
Distributed Tracing
```

### 12-4. Outbox 정책

현 단계:

- Outbox 테이블을 만들지 않는다.
- Outbox Publisher를 만들지 않는다.
- Debezium/CDC를 붙이지 않는다.

실무 단계:

- 도메인 상태 변경과 Outbox 레코드 저장을 같은 DB 트랜잭션으로 묶는다.
- 별도 Publisher 또는 CDC가 Outbox를 Kafka로 발행한다.
- 발행 완료 후 상태 마킹 또는 삭제 정책을 둔다.

---

## 13. REST API 규칙

### 13-1. HTTP API는 서비스 내부 유스케이스를 호출한다

Controller는 Usecase만 호출한다.

금지:

- Controller에서 Repository 직접 호출
- Controller에서 KafkaTemplate 직접 호출
- Controller에서 Domain 내부 컬렉션 직접 수정
- Controller에서 다른 서비스 API로 상태 변경 요청

### 13-2. API 경로는 원자료 호환을 우선한다

#### rental-service

```text
POST /api/RentalCard/
GET  /api/RentalCard/{id}
GET  /api/RentalCard/{id}/rentbook
GET  /api/RentalCard/{id}/returnbook
POST /api/RentalCard/rent
POST /api/RentalCard/return
POST /api/RentalCard/overdue
POST /api/RentalCard/clearoverdue
```

#### book-service

```text
POST /api/book
GET  /api/book/{no}
POST /api/book/{no}/available      optional
POST /api/book/{no}/unavailable    optional
```

#### member-service

```text
POST /api/Member/
GET  /api/Member/{no}
GET  /api/Member/by-id/{id}        테스트 편의용
POST /api/Member/{id}/points/save  테스트 편의용
POST /api/Member/{id}/points/use   테스트 편의용
```

#### bestbook-service

```text
GET /api/books
GET /api/books/{id}
```

---

## 14. Kafka 메시지 계약 규칙

### 14-1. common-events 모듈

서비스 간 메시지 계약은 `common-events`에 둔다.

```text
com.example.library.common.event
com.example.library.common.vo
```

포함:

```text
IDName
Item
ItemRented
ItemReturned
OverdueCleared
EventResult
EventType
PointUseCommand
```

금지:

- common-events에 도메인 비즈니스 로직 작성
- common-events에 Repository 작성
- common-events에 Kafka Producer/Consumer 작성
- common-events에 서비스별 Usecase 작성

### 14-2. 메시지 메타데이터

모든 Event/Command 메시지는 아래 필드를 가진다.

```text
eventId
correlationId
occurredAt
```

용도:

| 필드 | 용도 |
|---|---|
| `eventId` | 멱등 처리, 중복 방지 |
| `correlationId` | 요청-결과 추적 |
| `occurredAt` | 이벤트 발생 시각 |

### 14-3. Kafka 직렬화 규칙

- Producer value는 JSON 직렬화한다.
- Consumer는 `ConsumerRecord<String, String>`로 받고 ObjectMapper로 역직렬화할 수 있다.
- ObjectMapper에는 JavaTimeModule을 등록한다.
- `FAIL_ON_UNKNOWN_PROPERTIES=false`로 설정한다.
- 메시지 클래스는 기본 생성자, 전체 생성자, getter, setter를 제공한다.

---

## 15. 장애 유발 테스트 규칙

SAGA 테스트를 위해 실패 유발 설정을 둔다.

### 15-1. book-service

```yaml
app:
  failure:
    force-rent-fail: false
    force-return-fail: false
```

규칙:

- `force-rent-fail=true`: `rental_rent` 처리 시 실패 EventResult 발행
- `force-return-fail=true`: `rental_return` 처리 시 실패 EventResult 발행

### 15-2. member-service

```yaml
app:
  failure:
    force-overdue-clear-fail: false
```

규칙:

- `force-overdue-clear-fail=true`: `overdue_clear` 처리 시 실패 EventResult 발행

### 15-3. 실패 시에도 Result Event를 발행한다

금지:

```text
catch에서 로그만 찍고 끝내기
예외만 throw하고 끝내기
```

필수:

```text
catch에서 EventResult.successed=false 설정
reason 기록
rental_result 발행
```

---

## 16. 금지 사항

### 16-1. Hexagonal 금지 규칙

| # | 금지 규칙 | 이유 |
|---|---|---|
| H1 | Domain에서 Repository 직접 호출 금지 | Port/Adapter 분리 위반 |
| H2 | Domain에서 KafkaTemplate 직접 사용 금지 | 메시징 기술 의존 |
| H3 | Domain에서 RedisTemplate 직접 사용 금지 | 캐시/상태 기술 의존 |
| H4 | Controller에서 Repository 직접 호출 금지 | Application 우회 |
| H5 | Controller에서 Domain Entity 직접 반환 금지 | API 스펙과 도메인 결합 |
| H6 | InputPort에서 도메인 규칙을 직접 구현 금지 | 비즈니스 규칙은 Domain에 위치 |
| H7 | Framework Adapter끼리 직접 의존 금지 | Adapter 계층 결합 증가 |
| H8 | 다른 서비스 DB 직접 조회 금지 | 데이터 소유권 침해 |

### 16-2. DTO 금지 규칙

| # | 금지 규칙 | 이유 |
|---|---|---|
| D1 | Domain Entity에 `toResponse()` 금지 | Domain이 API 계층을 알게 됨 |
| D2 | API Request DTO를 Event DTO로 직접 사용 금지 | API 스펙과 메시지 스키마 결합 |
| D3 | Event DTO를 API Response로 직접 반환 금지 | 메시지 계약 노출 |
| D4 | Response DTO에 비즈니스 규칙 구현 금지 | Response는 표현/포맷팅만 담당 |
| D5 | Application DTO에 Web Validation 어노테이션 남발 금지 | Adapter 관심사 침투 |

### 16-3. EDA 금지 규칙

| # | 금지 규칙 | 이유 |
|---|---|---|
| E1 | 서비스 간 상태 변경을 동기 REST로 직접 처리 금지 | 서비스 결합도 증가 |
| E2 | 조회를 Kafka 비동기로 처리 금지 | 조회는 즉시 응답 필요 |
| E3 | 하나의 토픽에 여러 목적 이벤트 혼합 금지 | 소비자 복잡도 증가 |
| E4 | 이벤트에 소비자 특화 필드 추가 금지 | 발행자가 소비자를 알게 됨 |
| E5 | EventResult 없이 BookMS 처리 성공/실패를 끝내기 금지 | RentalMS가 보상 판단 불가 |
| E6 | MemberMS의 overdue_clear 처리 결과 누락 금지 | 정지해제 SAGA 판단 불가 |
| E7 | BestBookMS가 rental_result 발행 금지 | 베스트도서는 단방향 소비자 |
| E8 | PointUseCommand를 Domain Event처럼 취급 금지 | 커맨드와 이벤트 의미 혼동 |
| E9 | eventId 없는 Kafka 메시지 금지 | 멱등 처리 불가 |
| E10 | Consumer 멱등성 없이 상태 변경 금지 | 중복 메시지 시 데이터 오염 |

### 16-4. SAGA 금지 규칙

| # | 금지 규칙 | 이유 |
|---|---|---|
| S1 | `successed=true`인데 보상 실행 금지 | 정상 처리 롤백 위험 |
| S2 | `successed=false`인데 보상 누락 금지 | 분산 정합성 깨짐 |
| S3 | RENT 실패 시 point_use 누락 금지 | 회원 포인트 중복 적립 가능 |
| S4 | RETURN 실패 시 point_use 누락 금지 | 회원 포인트 중복 적립 가능 |
| S5 | OVERDUE 실패 시 point_use 발행 금지 | 원자료상 별도 포인트 사용취소 이벤트 없음 |
| S6 | 보상 메서드에서 save 누락 금지 | DB에 원복 상태 미반영 |
| S7 | 중복 EventResult에 보상 중복 실행 금지 | Redis 멱등 처리 필요 |

### 16-5. 실전 보강 항목 관련 금지 규칙

| # | 금지 규칙 | 이유 |
|---|---|---|
| P1 | Outbox를 구현하지 않았는데 구현했다고 문서화 금지 | 신뢰성 오해 |
| P2 | DLQ를 만들지 않았는데 장애 격리 완료라고 표현 금지 | 장애 대응 오해 |
| P3 | correlationId만 넣고 분산 추적 구현이라고 표현 금지 | tracing 시스템 미연동 |
| P4 | retry/backoff 설정 없이 재시도 전략 구현이라고 표현 금지 | 운영 안정성 오해 |
| P5 | Choreography를 구현해 놓고 Orchestration이라고 표현 금지 | SAGA 유형 혼동 |

---

## 17. 코드 리뷰 체크리스트

### 17-1. DDD / Hexagonal

- [ ] Aggregate Root가 상태 변경의 진입점인가?
- [ ] 도메인 규칙이 InputPort가 아니라 Domain에 있는가?
- [ ] InputPort가 `@Transactional` 경계인가?
- [ ] OutputPort가 비즈니스 필요 기준으로 정의되어 있는가?
- [ ] JPA Adapter가 OutputPort를 구현하는가?
- [ ] Kafka Producer가 EventOuputPort를 구현하는가?
- [ ] Controller가 Usecase만 호출하는가?
- [ ] 서비스 간 DB 직접 조회가 없는가?

### 17-2. DTO

- [ ] Controller가 Domain Entity를 직접 반환하지 않는가?
- [ ] Request/Response DTO와 Event DTO가 분리되어 있는가?
- [ ] `mapToDTO`, `from`, `toCommand` 위치가 규칙에 맞는가?
- [ ] Kafka 메시지 클래스에 기본 생성자/getter/setter가 있는가?
- [ ] JPA Entity/Embeddable에 필요한 기본 생성자가 있는가?

### 17-3. EDA

- [ ] `rental_rent`에는 `ItemRented`만 흐르는가?
- [ ] `rental_return`에는 `ItemReturned`만 흐르는가?
- [ ] `overdue_clear`에는 `OverdueCleared`만 흐르는가?
- [ ] `rental_result`에는 `EventResult`만 흐르는가?
- [ ] `point_use`에는 `PointUseCommand`만 흐르는가?
- [ ] 이벤트 크기가 최소화되어 있는가?
- [ ] 이벤트에 소비자 특화 필드가 없는가?

### 17-4. SAGA

- [ ] BookMS가 대여/반납 처리 후 성공/실패 EventResult를 항상 발행하는가?
- [ ] MemberMS가 정지해제 처리 후 성공/실패 EventResult를 항상 발행하는가?
- [ ] RentalMS가 `successed=false`일 때만 보상하는가?
- [ ] RENT 실패 시 `cancleRentItem`과 `point_use`가 실행되는가?
- [ ] RETURN 실패 시 `cancleReturnItem`과 `point_use`가 실행되는가?
- [ ] OVERDUE 실패 시 `cancleMakeAvailableRental`만 실행되는가?
- [ ] BestBookMS가 SAGA 응답/보상에 참여하지 않는가?
- [ ] 모든 Consumer에 Redis 멱등 처리가 있는가?

### 17-5. 운영 한계 문서화

- [ ] README에 Outbox 미구현 한계가 적혀 있는가?
- [ ] README에 DLQ 미구현 한계가 적혀 있는가?
- [ ] README에 Retry/Backoff 미구현 한계가 적혀 있는가?
- [ ] README에 분산 추적 미구현 한계가 적혀 있는가?
- [ ] README에 현재 SAGA가 Choreography임이 적혀 있는가?

---

## 18. 전체 시퀀스 요약

### 18-1. 대여 정상 케이스

```text
Client
  → RentalMS POST /api/RentalCard/rent
  → RentalCard.rentItem
  → Rental DB save
  → Kafka rental_rent: ItemRented

Kafka rental_rent
  → BookMS consumeRental
  → Book.makeUnAvailable
  → Book DB save
  → Kafka rental_result: EventResult{RENT,true}

Kafka rental_result
  → RentalMS consume
  → successed=true, 종료

Kafka rental_rent
  → MemberMS consumeRent
  → Member.savePoint(+10)

Kafka rental_rent
  → BestBookMS consume
  → BestBook.increseBestBookCount
```

### 18-2. 대여 실패 보상 케이스

```text
RentalMS
  → ItemRented 발행

BookMS
  → makeUnAvailable 실패
  → Kafka rental_result: EventResult{RENT,false}

RentalMS
  → rental_result 소비
  → cancleRentItem
  → Kafka point_use: PointUseCommand

MemberMS
  → point_use 소비
  → Member.usePoint
```

### 18-3. 반납 실패 보상 케이스

```text
RentalMS
  → ItemReturned 발행

BookMS
  → makeAvailable 실패
  → Kafka rental_result: EventResult{RETURN,false}

RentalMS
  → cancleReturnItem
  → Kafka point_use: PointUseCommand

MemberMS
  → point_use 소비
  → Member.usePoint
```

### 18-4. 정지해제 실패 보상 케이스

```text
RentalMS
  → makeAvailableRental
  → Kafka overdue_clear: OverdueCleared

MemberMS
  → usePoint 실패
  → Kafka rental_result: EventResult{OVERDUE,false}

RentalMS
  → cancleMakeAvailableRental
  → point_use 발행 안 함
```

---

## 19. 최종 원칙 요약

이 프로젝트의 아키텍처 핵심은 다음이다.

```text
도메인은 규칙을 가진다.
Application은 흐름을 제어한다.
Port는 기술을 추상화한다.
Framework Adapter는 기술을 구현한다.
Kafka Event는 서비스 간 상태 변화를 전파한다.
EventResult는 SAGA 성공/실패를 알려준다.
RentalMS는 실패 결과를 보고 보상한다.
Redis는 중복 메시지로 인한 중복 보상을 막는다.
Outbox, DLQ, Tracing, Retry/Backoff, Orchestration은 이번 범위에서 구현하지 않고 향후 실전 보강점으로 둔다.
```
