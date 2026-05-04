# Architecture Rule 적용 리팩토링 계획안

## 1. 목적

이 문서는 `docs/architecture-rule-eda.md`의 DDD, Hexagonal Architecture, DTO 분리, Port/Adapter, EDA 명명 규칙을 현재 `library-rental-eda` 프로젝트에 적용하기 위한 리팩토링 계획이다.

단, 루트 `AGENTS.md`의 프로젝트 예외 규칙이 항상 우선한다.

## 2. 적용 기준

### 2-1. 우선순위

1. 루트 `AGENTS.md`
2. 현재 프로젝트의 동작과 테스트
3. `docs/architecture-rule-eda.md`
4. 일반적인 Spring Boot / JPA / Kafka 관례

### 2-2. 적용 대상

- `common-events`
- `book-service`
- `member-service`
- `rental-service`
- `bestbook-service`

### 2-3. 제외 대상

`architecture-rule-eda.md`에 언급되어 있더라도 아래 항목은 구현하지 않는다.

- Outbox pattern
- DLQ / DLT
- Custom Kafka retry/backoff
- Distributed tracing
- SAGA orchestration code
- 서비스 간 직접 HTTP 호출

기존 코드에 있는 Kafka result-event 기반 보상 흐름은 신규 Orchestrator 없이 현재 범위 안에서 정리한다.

## 3. 현재 상태 요약

### 3-1. 모듈 현황

| 모듈 | Java 파일 수 | 현재 역할 |
|------|-------------:|-----------|
| `common-events` | 8 | 서비스 간 공유 이벤트/VO |
| `book-service` | 31 | 도서 등록, 조회, 대여 가능/불가 상태 변경 |
| `member-service` | 33 | 회원 등록, 조회, 포인트 적립/사용 |
| `rental-service` | 39 | 대여카드, 대여/반납/연체/보상 흐름 |
| `bestbook-service` | 20 | 대여 이벤트 기반 베스트 도서 read model |

### 3-2. 기준선

현재 기준선 테스트:

```text
.\gradlew.bat test
BUILD SUCCESSFUL
```

리팩토링은 이 기준선을 유지하면서 단계별로 진행한다.

### 3-3. 보완 작업 반영 현황

현재 보완 작업으로 다음 항목을 반영했다.

- `AGENTS.md`와 이 계획안에 `infrastructure` 목표 구조와 경계 규칙 추가
- `application/port/out` outbound port 보강
- `book/member/rental-service`의 `adapter/out/persistence` JPA Entity, Mapper, Spring Data Repository, Persistence Adapter 보강
- `bestbook-service`의 `adapter/out/persistence` MongoDB Document, MongoRepository, Mapper, Persistence Adapter 보강
- `adapter/out/messaging` Kafka Producer Adapter 보강
- Kafka Consumer의 처리 분기를 `application/service` UseCase 구현체로 이동
- `adapter/out` 경로가 Git에서 무시되지 않도록 `.gitignore` 패턴 보정

확인 결과 `domain/model`에는 JPA 어노테이션이 남아 있지 않다.

## 4. 주요 차이점

### 4-1. 패키지 역할 명명 차이

현재 구조:

```text
application/
├── inputport/    # 실제 구현체, @Service
├── outputport/   # outbound port interface
└── usecase/      # inbound port interface

framework/
├── jpaadapter/
├── kafkaadapter/
└── web/
```

`architecture-rule-eda.md` 기준 목표 구조:

```text
application/
├── dto/
├── port/
│   ├── in/
│   └── out/
└── service/

adapter/
├── in/
│   ├── web/
│   └── messaging/consumer/
└── out/
    ├── messaging/
    └── persistence/

infrastructure/
├── messaging/
├── security/
└── common/

config/
```

### 4-2. Domain과 Persistence 분리

초기 분석 기준으로는 `domain/model`의 Aggregate 또는 VO가 JPA 어노테이션을 직접 가지고 있었지만, 보완 작업에서 persistence model을 adapter 영역으로 분리했다.

유지 목표:

- `domain/model`은 순수 Java 모델로 유지
- `book/member/rental-service`의 JPA Entity는 `adapter/out/persistence/entity`에 위치
- `bestbook-service`의 MongoDB Document는 `adapter/out/persistence/document`에 위치
- Domain Model과 persistence model 사이 Mapper 유지

### 4-3. DTO와 메시지의 mutable class 사용

현재 대부분의 API DTO, 이벤트, 커맨드가 mutable class이다.

대표 예:

- `BookInfoDTO`
- `BookOutPutDTO`
- `MemberInfoDTO`
- `RentalCardOutputDTO`
- `EventResult`
- `ItemRented`
- `PointUseCommand`

목표:

- API Request/Response DTO는 가능하면 `record`로 전환
- Application Command/Query/Result를 별도 `record`로 분리
- Event/Command/Result message는 의미별로 명확히 분리

### 4-4. Kafka Consumer 책임 과다

현재 Consumer는 다음 책임을 함께 가진다.

- 역직렬화
- Redis 기반 멱등성 체크
- use case 호출
- 실패 result event 생성
- 보상 흐름 분기

목표:

- Consumer는 수신, 역직렬화, 최소 검증, 위임 중심으로 축소
- 비즈니스 판단은 Application Service 또는 Domain으로 이동
- Kafka 발행은 outbound port 구현체를 통해서만 수행

### 4-5. 명명 오류와 용어 혼재

현재 오타 또는 혼재된 이름이 많다.

예:

- `OutPut` -> `Output`
- `Ouput` -> `Output`
- `Oupput` -> `Output`
- `Retun` -> `Return`
- `Retrun` -> `Return`
- `Usercase` -> `UseCase`
- `Classfication` -> `Classification`
- compensation method names use `cancel`
- `successed` -> `succeeded` 또는 `success`

목표:

- 내부 코드명은 표준 명명으로 정리
- 외부 API/JSON 필드 호환성이 필요한 경우에는 별도 호환 전략을 둔다

## 5. 목표 아키텍처

### 5-1. 서비스별 목표 패키지

각 서비스는 다음 구조를 목표로 한다.

```text
com.example.library.{domain}/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── dto/
│   │   └── messaging/
│   │       └── consumer/
│   └── out/
│       ├── messaging/
│       └── persistence/
│           ├── entity/
│           ├── mapper/
│           └── repository/
├── application/
│   ├── dto/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── service/
├── config/
├── domain/
│   ├── event/
│   ├── exception/
│   ├── model/
│   └── policy/
└── infrastructure/
    ├── messaging/
    ├── security/
    └── common/
```

### 5-2. 현재 구조에서 목표 구조로의 매핑

| 현재 | 목표 |
|------|------|
| `application/usecase` | `application/port/in` |
| `application/outputport` | `application/port/out` |
| `application/inputport` | `application/service` |
| `framework/web` | `adapter/in/web` |
| `framework/web/dto` | `adapter/in/web/dto` |
| `framework/kafkaadapter/*Consumers` | `adapter/in/messaging/consumer` |
| `framework/kafkaadapter/*Producer` | `adapter/out/messaging` |
| `framework/jpaadapter` | `adapter/out/persistence` |
| `domain/model`의 JPA 어노테이션 | `adapter/out/persistence/entity`로 분리 |
| Kafka Serializer/Deserializer, 메시징 기술 지원 | `infrastructure/messaging` |
| Security Filter, 인증/인가 기술 지원 | `infrastructure/security` |
| 공통 기술 유틸리티 | `infrastructure/common` |
| Spring Bean 조립용 `@Configuration` | `config` |

### 5-3. Infrastructure 영역 목표와 경계

`Infrastructure`는 도메인 로직이나 유스케이스가 아니라 프레임워크 사용을 돕는 기술 지원 영역이다.

포함 대상:

- Kafka serializer/deserializer, 메시징 설정 보조 클래스, 공통 메시징 유틸리티
- Spring Security filter/provider 등 보안 기술 지원 코드
- Redis, ObjectMapper, clock/id generator 등 여러 adapter가 공유하는 기술 유틸리티
- MongoDB client/custom converter 등 여러 adapter가 공유하는 기술 유틸리티
- 프레임워크 통합을 위한 공통 helper

제외 대상:

- Kafka Consumer: `adapter/in/messaging/consumer`
- Kafka Producer: `adapter/out/messaging`
- JPA Entity, MongoDB Document, Mapper, Spring Data Repository: `adapter/out/persistence`
- Application Service, Command Handler, 보상 흐름 분기: `application/service`
- Domain Model, Domain Event 생성 규칙: `domain`

`config`와의 경계:

- `config`는 Spring `@Configuration`과 Bean wiring을 담당한다.
- `infrastructure`는 Bean으로 조립될 기술 지원 클래스를 담는다.
- 단순한 `KafkaConfig`, `SecurityConfig`, `QueryDslConfig`는 `config`에 남길 수 있다.
- Serializer, Deserializer, Security Filter, 공통 Kafka/Redis helper처럼 재사용 가능한 기술 구현은 `infrastructure`로 분리한다.

## 6. 단계별 리팩토링 계획

### Phase 0. 기준선 고정

목표:

- 현재 동작과 테스트 기준선을 고정한다.

작업:

- `.\gradlew.bat test` 실행
- 현재 API, topic, event payload 목록 정리
- 리팩토링 중 변경하면 안 되는 외부 계약 식별

완료 기준:

- 전체 테스트 성공
- 리팩토링 전/후 비교 기준 확보

### Phase 1. 명명과 패키지 역할 정리

목표:

- 동작 변경 없이 코드 역할을 명확히 한다.

작업:

- `Usecase` -> `UseCase`
- `OutPut/Ouput/Oupput` -> `Output`
- `Retun/Retrun` -> `Return`
- compensation method names use `cancel`
- `Classfication` -> `Classification`
- `successed` -> `succeeded` 또는 `success`
- 기존 public API나 JSON 필드에 영향이 있으면 호환 메서드 또는 Jackson alias를 둔다.

완료 기준:

- 전체 테스트 성공
- 오타 기반 타입명이 신규 코드에서 사용되지 않음

권장 순서:

1. `book-service`
2. `member-service`
3. `bestbook-service`
4. `rental-service`
5. `common-events`

### Phase 2. Application 계층 재배치

목표:

- `architecture-rule-eda.md`의 Port/Service 구조에 맞춘다.

작업:

- `application/usecase` 인터페이스를 `application/port/in`으로 이동
- `application/outputport` 인터페이스를 `application/port/out`으로 이동
- `application/inputport` 구현체를 `application/service`로 이동
- 구현체 이름을 `*Service` 또는 `*CommandHandler`로 정리
- Controller와 Consumer는 `application.port.in`만 의존하도록 정리
- Persistence/Kafka Adapter는 `application.port.out`만 구현하도록 정리

완료 기준:

- Adapter가 Application Service 구현체에 직접 의존하지 않음
- Application Service가 Adapter 구현체를 직접 참조하지 않음
- 전체 테스트 성공

### Phase 2-1. Infrastructure 영역 정리

목표:

- `adapter`, `application`, `domain`, `config`에 섞인 기술 지원 코드를 `infrastructure` 영역으로 분리한다.
- Producer/Consumer/JPA Adapter와 Infrastructure의 책임을 혼동하지 않도록 경계를 명확히 한다.

작업:

- 각 서비스에 필요한 경우 `infrastructure/messaging`, `infrastructure/security`, `infrastructure/common` 패키지를 추가한다.
- Kafka serializer/deserializer, 공통 메시징 helper, 공통 Redis/Kafka 기술 유틸리티는 `infrastructure/messaging`으로 분리한다.
- Security filter/provider 등 보안 기술 구현은 `infrastructure/security`로 분리한다.
- 여러 adapter에서 공유하는 순수 기술 유틸리티는 `infrastructure/common`으로 분리한다.
- `KafkaConfig`, `SecurityConfig`, `QueryDslConfig`처럼 Bean wiring 중심의 `@Configuration`은 `config`에 남기되, 내부 구현이 커지면 infrastructure support 클래스로 추출한다.
- Kafka Consumer/Producer/JPA Repository/MongoRepository 구현은 infrastructure로 이동하지 않고 각각 `adapter/in/messaging/consumer`, `adapter/out/messaging`, `adapter/out/persistence`에 유지한다.

주의:

- Infrastructure에 비즈니스 규칙, 보상 분기, use case orchestration을 넣지 않는다.
- Domain/Application은 Infrastructure를 import하지 않는다.
- Outbox, DLQ/DLT, custom retry/backoff, distributed tracing은 도입하지 않는다.

완료 기준:

- `infrastructure`에는 기술 지원 코드만 존재
- `config`는 Bean 조립 책임 중심
- Kafka Consumer/Producer와 JPA Adapter가 올바른 adapter 패키지에 남아 있음
- 전체 테스트 성공

### Phase 3. DTO 분리

목표:

- API DTO, Application DTO, Domain Model을 분리한다.

작업:

- `adapter/in/web/dto`에는 Request/Response DTO만 둔다.
- `application/dto`에는 Command/Query/Result record를 둔다.
- Request DTO에는 `toCommand()` 또는 `toQuery()`를 둔다.
- Response DTO에는 `from(Result)` 정적 팩토리를 둔다.
- Application Result에는 `from(Domain)` 정적 팩토리를 둔다.
- Controller가 Domain Model을 직접 응답 DTO로 변환하는 구조를 줄인다.

예시 목표 흐름:

```text
Web Request DTO
  -> Application Command
  -> UseCase
  -> Application Result
  -> Web Response DTO
```

완료 기준:

- Controller의 반환 타입은 Web Response DTO
- Controller가 Domain Model의 내부 컬렉션/상태를 직접 탐색하지 않음
- DTO는 가능한 범위에서 `record` 사용

### Phase 4. Domain과 Persistence 분리

목표:

- Domain Layer를 순수 Java로 만든다.

작업:

- `domain/model`의 `@Entity`, `@Embeddable`, `@Column`, `@Id`, `@ElementCollection` 제거
- `book/member/rental-service`는 `adapter/out/persistence/entity`에 JPA Entity 생성
- `bestbook-service`는 `adapter/out/persistence/document`에 MongoDB `@Document` 생성
- `adapter/out/persistence/mapper`에 Domain/Persistence Mapper 생성
- Spring Data Repository는 JPA Entity 또는 MongoDB Document만 다루도록 변경
- Outbound Port는 Domain Model을 반환/저장하도록 유지

모듈별 난이도:

| 모듈 | 난이도 | 이유 |
|------|--------|------|
| `bestbook-service` | 낮음 | 단순 read model |
| `book-service` | 중간 | `BookDesc` embedded 분리 필요 |
| `member-service` | 중간 | `Email`, `Password`, `Point`, `IDName` 분리 필요 |
| `rental-service` | 높음 | `RentalCard`, `RentItem`, `ReturnItem`, `LateFee`, element collection 분리 필요 |
| `common-events` | 높음 | 여러 서비스가 공유하므로 변경 영향이 큼 |

권장 순서:

1. `bestbook-service`로 패턴 검증
2. `book-service`
3. `member-service`
4. `rental-service`
5. `common-events`의 JPA 의존 제거

완료 기준:

- `domain/model`에 Jakarta Persistence import 없음
- Domain 테스트는 Spring context 없이 실행 가능
- JPA 관련 코드는 persistence adapter 하위에만 존재

### Phase 5. EDA 메시지 정리

목표:

- Command Message, Domain Event, Result Event의 의미를 명확히 분리한다.

작업:

- `common-events`의 메시지 타입을 목적별로 재정리
- 명령은 `*Command` 또는 `*CommandMessage`
- 발생 사실은 과거형 `*Event`
- 처리 결과는 `*Result` 또는 `*ResultEvent`
- `eventId`, `correlationId`, 필요 시 `commandId`를 일관되게 사용
- `EventType`은 Result Event 분기 용도로 유지하되 의미가 섞이지 않게 정리
- Kafka Producer는 `EventOutputPort` 같은 outbound port를 통해서만 호출

주의:

- Outbox는 도입하지 않는다.
- DLQ/DLT는 도입하지 않는다.
- Custom retry/backoff는 도입하지 않는다.
- 기존 topic 이름을 변경해야 하면 migration 계획을 별도로 둔다.

완료 기준:

- 메시지 이름만 보고 Command/Event/Result 구분 가능
- Kafka consumer가 실패 result를 일관된 포맷으로 발행
- 서비스 간 HTTP 호출 없음

### Phase 6. Kafka Consumer 책임 축소

목표:

- Consumer를 Inbound Adapter 역할에 집중시킨다.

작업:

- Consumer의 책임을 수신, 역직렬화, 멱등성 체크, use case 위임으로 제한
- Result Event 생성 로직은 Application Service 또는 별도 factory로 이동
- 보상 분기는 신규 Saga Orchestrator 없이 현재 result-event reaction 범위에서만 정리
- Redis 멱등성 키 생성 규칙을 공통화할지 검토

완료 기준:

- Consumer에 핵심 비즈니스 규칙 없음
- Consumer가 Spring Data Repository를 직접 사용하지 않음
- Consumer가 KafkaTemplate을 직접 사용하지 않음

### Phase 7. API 응답 정책 정리

목표:

- 동기 요청과 비동기 이벤트 발행 요청을 구분한다.

작업:

- 단일 서비스 내부에서 완료되는 조회/명령은 기존 동기 응답 유지 가능
- 다른 서비스와 협업하는 명령은 `202 Accepted`와 추적 ID 응답을 검토
- 기존 API 호환성이 필요하면 endpoint 변경은 별도 단계로 분리
- URL casing은 장기적으로 lower-case/kebab-case로 정리 검토

주의:

- 서비스 간 조회를 위해 HTTP client를 추가하지 않는다.
- 필요한 read model은 Kafka event 기반으로 유지한다.

완료 기준:

- API 응답이 실제 처리 방식과 일치
- 비동기 명령은 추적 가능한 ID를 반환

### Phase 8. 테스트 보강

목표:

- 리팩토링으로 구조는 바뀌지만 동작은 유지되는지 확인한다.

작업:

- Domain 단위 테스트 보강
- Application Service 단위 테스트 보강
- Persistence Mapper 테스트 추가
- Kafka Consumer 위임 테스트 추가
- 주요 command/event/result 흐름 테스트 추가

완료 기준:

- `.\gradlew.bat test` 성공
- Domain 테스트가 Spring 없이 실행 가능
- 리팩토링된 모듈별 주요 유스케이스가 테스트로 보호됨

## 7. 모듈별 상세 계획

### 7-1. bestbook-service

현재 문제:

- `service/BestBookService`가 repository 구현체에 직접 의존
- `domain/model/BestBook`이 persistence model과 결합될 위험
- Controller가 Service를 직접 사용

계획:

- `application/port/in/BestBookQueryUseCase`, `RecordBestBookRentUseCase` 생성
- `application/port/out/BestBookPort` 생성
- `application/service/BestBookService`로 이동
- `adapter/out/persistence/document`에 MongoDB `@Document` 구성
- `adapter/out/persistence/repository`에 `MongoRepository` 구성
- `spring.data.mongodb` 설정과 로컬 MongoDB compose 서비스 구성
- `adapter/in/messaging/consumer`에서 `RecordBestBookRentUseCase`만 호출
- DTO를 record로 전환

우선 적용 후보로 적합하다.

### 7-2. book-service

현재 문제:

- `Book`이 Domain과 JPA Entity를 겸함
- `BookDesc`가 JPA Embeddable
- UseCase 인터페이스와 구현체 패키지명이 문서 기준과 반대에 가까움
- DTO class와 `OutPut` 명명 오류 존재

계획:

- `BookUseCase` 계열을 `application/port/in`으로 이동
- `BookOutputPort`를 `application/port/out`으로 이동
- `BookApplicationService` 계열로 구현체 재배치
- `BookJpaEntity`, `BookDescJpaEmbeddable` 또는 flat entity 설계
- `BookMapper` 추가
- `BookInfoDTO`, `BookOutputDTO`를 Request/Response record로 정리
- Kafka Consumer는 대여/반납 이벤트 수신 후 UseCase 위임만 수행

### 7-3. member-service

현재 문제:

- `Member`와 VO들이 JPA Entity/Embeddable
- 포인트 변경 로직과 Kafka result 발행 흐름이 Consumer/Producer와 가까움
- DTO class와 `OutPut` 명명 오류 존재

계획:

- `MemberUseCase` 계열을 `application/port/in`으로 이동
- `MemberOutputPort`를 `application/port/out`으로 이동
- 순수 `Member`, `Email`, `Password`, `Point` 모델과 JPA Entity 분리
- 포인트 사용/적립 command/result 흐름의 메시지 명명 정리
- Kafka Consumer는 이벤트별 UseCase 위임만 수행

### 7-4. rental-service

현재 문제:

- 가장 복잡한 Aggregate인 `RentalCard`가 JPA Entity
- `RentItem`, `ReturnItem`, `LateFee`, `RentalCardNo`가 JPA Embeddable
- 보상 메서드 오타와 중복 메서드 존재
- `EventOuputPort`, `RentalCardOuputPort`, `ReturnItemUsercase` 등 명명 오류 존재
- Controller가 Domain 객체 기반으로 Response DTO를 직접 구성

계획:

- 가장 마지막에 리팩토링한다.
- 먼저 명명 오류를 호환 메서드와 함께 정리한다.
- `RentalCard` 순수 domain 모델과 `RentalCardJpaEntity` 분리
- `RentItemJpaEmbeddable`, `ReturnItemJpaEmbeddable`, `LateFeeJpaEmbeddable` 분리
- `RentalCardMapper` 추가
- `EventOutputPort`를 명확히 정리
- `RentalKafkaProducer`는 outbound messaging adapter로 이동
- `RentalEventConsumers`는 inbound messaging consumer로 이동
- Result Event 기반 보상은 유지하되 Saga Orchestrator로 확장하지 않는다.

### 7-5. common-events

현재 문제:

- 공유 VO인 `IDName`, `Item`에 JPA `@Embeddable`, `@Column`이 있음
- 이벤트/커맨드/result가 mutable class
- `EventResult.successed`처럼 명명이 어색함

계획:

- JPA 의존 제거를 최종 단계에서 수행
- 각 서비스 persistence adapter에 필요한 Embeddable을 별도로 둔다.
- 공유 계약은 Java record 중심으로 전환
- 호환성 문제를 피하려면 기존 class를 바로 제거하지 않고, 신규 record 계약 도입 후 소비자/생산자를 순차 전환한다.

## 8. 권장 실행 순서

1. 기준선 테스트 고정
2. 명명 오류 정리
3. `bestbook-service` 구조 리팩토링으로 패턴 검증
4. `book-service` 리팩토링
5. `member-service` 리팩토링
6. `rental-service` 리팩토링
7. `common-events` 메시지/VO 계약 정리
8. 전체 테스트 및 통합 실행 확인

## 9. 리스크와 대응

| 리스크 | 영향 | 대응 |
|--------|------|------|
| JPA Entity 분리로 QueryDSL Q-class 변경 | 컴파일 오류 | 모듈별로 작게 분리하고 즉시 테스트 |
| bestbook-service MongoDB 전환 | 실행 환경 누락 | `spring.data.mongodb`와 compose MongoDB 서비스를 함께 유지 |
| shared event record 전환 | 모든 서비스 직렬화 영향 | 기존 class 유지 후 단계적 전환 |
| package rename 대량 발생 | import 충돌 | 서비스 단위로 적용 |
| API DTO record 전환 | Jackson binding 영향 | 요청 DTO부터 테스트와 함께 전환 |
| 보상 흐름 정리 | rental/member/book 연동 영향 | topic/payload 유지 후 내부 구조만 변경 |
| 오타 명명 수정 | 기존 테스트/참조 깨짐 | 호환 메서드 유지 후 제거 단계 분리 |

## 10. 완료 정의

리팩토링 완료 기준:

- 전체 테스트 `.\gradlew.bat test` 성공
- 서비스 간 직접 HTTP client 없음
- Domain Layer에서 Spring/JPA/Kafka import 제거
- JPA Entity와 MongoDB Document는 persistence adapter 하위에만 위치
- Controller와 Consumer는 Inbound Port만 의존
- Application Service는 Outbound Port만 의존
- Kafka Producer는 Outbound Messaging Adapter에서만 `KafkaTemplate` 사용
- Infrastructure에는 메시징/보안/공통 기술 지원 코드만 위치하고 비즈니스 로직이 없음
- `config`는 Spring Bean wiring 중심으로 유지
- Command/Event/Result 메시지 이름과 목적이 분리됨
- Outbox, DLQ/DLT, custom retry/backoff, distributed tracing, Saga Orchestrator 미도입

## 11. 첫 번째 실제 작업 제안

첫 구현 단계는 `bestbook-service`를 대상으로 한다.

이유:

- 파일 수가 적다.
- 비즈니스 규칙이 단순하다.
- Kafka event 기반 read model 성격이 명확하다.
- Port/Adapter 구조 전환 패턴을 검증하기 좋다.

첫 작업 범위:

1. `BestBookService`를 application service로 이동
2. inbound port와 outbound port 생성
3. persistence adapter가 outbound port 구현
4. Kafka consumer와 web controller가 inbound port만 의존하도록 변경
5. 테스트 실행

