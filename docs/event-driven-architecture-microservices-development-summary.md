# 이벤트 기반 아키텍처 적용 마이크로서비스 개발 분석 정리

## 문서 정보

- 원본 제목: 이벤트 기반 아키텍처 적용 마이크로서비스 개발
- 원본 분량: 123쪽
- 정리 목적: 도서관 대여 도메인을 예제로 한 MSA, DDD, Hexagonal Architecture, EDA, SAGA 구현 흐름을 프로젝트 문서로 재구성한다.

이 문서는 PDF 원문을 분석해 핵심 요구사항, 서비스별 구현 모델, 이벤트 흐름, Kafka 토픽, SAGA 보상 흐름을 정리한 것이다. PDF의 예제 코드는 Java 11, Maven, Spring Boot 2.7.4, H2, ZooKeeper 기반 Kafka, Springfox Swagger를 전제로 설명한다. 현재 `library-rental-eda` 프로젝트는 Java 21, Gradle, Spring Boot 3.3.7, MariaDB, MongoDB, Redis, Kafka KRaft를 사용하므로 마지막의 "현재 프로젝트 적용 기준"을 우선 적용한다.

## 전체 구성

| PDF 범위 | 주제 | 핵심 내용 |
|---|---|---|
| 1-8쪽 | 전체 아키텍처 및 구현 전략 | 요구사항, 서비스 매핑, Hexagonal Architecture, 도메인별 아키텍처 스타일 |
| 8-45쪽 | 대여 마이크로서비스 | RentalCard 애그리게이트, 대여/반납/연체/정지해제 유스케이스, JPA, REST API |
| 46-59쪽 | 도서 마이크로서비스 | Book 엔티티, 입고/이용가능/이용불가 처리, JPA, REST API |
| 59-73쪽 | 회원 마이크로서비스 | Member 엔티티, 포인트 적립/사용, 권한, JPA, REST API |
| 74-81쪽 | BEST 서적 마이크로서비스 | MongoDB 기반 읽기 모델, 대여 횟수 증가, REST API |
| 82-104쪽 | EDA 구현 | 도메인 이벤트, Kafka Producer/Consumer, 통합 테스트 시나리오 |
| 104-120쪽 | EDA-SAGA 구현 | 결과 이벤트, 보상 트랜잭션, 포인트 회수 커맨드, Choreography SAGA |
| 121-123쪽 | 배포 | Dockerfile, docker compose, MongoDB, Kafka, Kafka UI |

## 요구사항

### 회원 관리

- 회원을 등록한다.
- 특정 회원은 관리자 역할을 부여받는다.
- 회원은 시스템 사용을 위해 로그인하거나 로그아웃할 수 있다.

### 도서 관리

- 관리자는 도서분류정보를 등록, 수정, 삭제한다.
- 사내 도서관은 판교와 정자에 위치한다.
- 도서는 도서공급사가 공급하거나 기부될 수 있다.
- 입고된 도서는 사서에 의해 분류된다.
- 사서는 분류한 도서의 상태 초기값을 대여 가능으로 설정해 대여 가능하게 한다.
- 대여와 반납에 의해 도서 상태가 대여 가능, 대여 중으로 조정된다.

### 도서 대여 및 반납

- 사용자는 도서를 검색할 수 있다.
- 사용자는 베스트 대여 목록을 조회할 수 있다.
- 사용자는 대여 가능한 도서를 대여할 수 있다.
- 대여 조건은 2주, 1인당 5권 이내이다.
- 반납되지 않고 대여 기간이 지난 도서는 연체된다.
- 연체 시 연체 포인트가 1일 10포인트 부여된다.
- 1권이라도 연체되면 사용자는 대여 불가 상태가 된다.
- 사용자는 대여한 도서를 반납할 수 있다.
- 반납 시 연체료가 계산된다.
- 사용자는 대여한 도서 이력을 볼 수 있다.
- 사용자가 도서를 대여하거나 반납하면 사용자에게 10포인트가 적립된다.
- 연체가 있으면 대여할 수 없다.
- 대여된 도서는 모두 반납되어야 대여 정지를 해제할 수 있다.
- 포인트를 사용해 대여 정지를 해제할 수 있다.
- 포인트는 연체료 감면에 사용될 수 있다.
- 연체 포인트를 0으로 만들면 대출 가능 상태가 된다.

## 구현 전략

### 도메인 유형과 아키텍처 스타일

| 도메인 | 유형 | 원문 구현 전략 |
|---|---|---|
| 대여 | 핵심 도메인 | Hexagonal Architecture, Domain Model, Spring MVC, Spring Data JPA, RDB |
| 회원 | 일반 도메인 | Hexagonal Architecture, Domain Model, Spring MVC, Spring Data JPA, RDB |
| 도서 | 일반 도메인 | Hexagonal Architecture, Domain Model, Spring MVC, Spring Data JPA, RDB |
| BEST 도서 | 지원 도메인 | Layered Architecture + CQRS, Domain Model, Spring MVC, Spring Data, MongoDB |

원문은 핵심 도메인에는 도메인 모델 중심의 헥사고날 아키텍처를 적용하고, 지원 도메인인 BEST 도서에는 레이어드 아키텍처와 CQRS 성격의 읽기 모델을 적용한다.

### Hexagonal Architecture 구성

- 도메인 헥사곤: Aggregate, Entity, Value Object, Enum, Domain Event로 구성한다. 비즈니스 개념과 비즈니스 로직을 구현한다.
- 애플리케이션 헥사곤: Use Case 인터페이스, Input Port, Output Port로 구성한다. 트랜잭션 처리, 흐름 제어, 유스케이스 구현을 담당한다.
- 프레임워크 헥사곤: 입력 어댑터, 출력 어댑터로 구성한다. REST API, DB 입출력, 메시지 생산/소비를 담당한다.

원문 패키지 예시는 `application/inputport`, `application/outputport`, `application/usecase`, `domain/model`, `framework/jpaadapter`, `framework/kafkaadapter`, `framework/web` 구조를 사용한다. 현재 프로젝트에서는 `adapter/in`, `adapter/out`, `application/port/in`, `application/port/out` 구조로 보정한다.

## 대여 마이크로서비스

### 목적과 기술 구성

- 아키텍처 스타일: Hexagonal Architecture
- 비즈니스 로직 구현 패턴: Domain Model
- 원문 저장소: H2
- 원문 OR Mapper: Spring Data JPA
- 메시지 브로커: Kafka
- API 명세: Swagger

### 도메인 모델

| 모델 | 유형 | 핵심 속성 |
|---|---|---|
| `RentalCard` | Aggregate Root | `RentalCardNo`, `IDName member`, `RentStatus`, `LateFee`, `List<RentItem>`, `List<ReturnItem>` |
| `RentalCardNo` | VO | 연도 + UUID 기반 도서카드 번호 |
| `IDName` | VO | `id`, `name` |
| `RentStatus` | Enum | `RENT_AVAILABLE`, `RENT_UNAVAILABLE` |
| `LateFee` | VO | 연체 포인트, `addPoint`, `removePoint` |
| `RentItem` 또는 `RentalItem` | Entity 성격 | 대여 품목, 대여일, 연체 여부, 반납 예정일 |
| `Item` | VO | 품목 번호, 제목 |
| `ReturnItem` | VO | 대여 품목, 반납일 |

원문은 설계 초기에 `RentalItem`을 VO로 보았으나, 연체 여부가 변경되므로 불변성이 깨진다고 판단한다. 따라서 대여 중인 아이템이면서 연체 상태를 포함하는 개념으로 보고 엔티티 성격으로 재정의한다.

### 핵심 비즈니스 로직

- `createRentalCard`
  - 도서카드번호를 채번한다.
  - 카드 주인을 명기한다.
  - 최초 상태를 대여 가능으로 둔다.
  - 연체료를 0으로 설정한다.

- `rentItem`
  - 대여 가능 상태인지 확인한다.
  - 전체 대여 품목이 5권 이내인지 확인한다.
  - 입력된 아이템으로 대여 품목을 생성한다.
  - 대여일은 현재일, 연체 여부는 false, 반납 예정일은 2주 후로 설정한다.
  - 대여 품목을 RentalCard의 대여 목록에 추가한다.

- `returnItem`
  - 도서카드의 대여 목록에서 해당 대여 품목을 찾는다.
  - 반납 예정일과 실제 반납일을 비교한다.
  - 연체 시 연체일 수 * 10포인트로 연체료를 계산한다.
  - 반납 품목을 생성해 반납 목록에 추가한다.
  - 대여 목록에서 해당 품목을 제거한다.

- `overdueItem`
  - 시스템 배치나 스케줄로 수행될 성격의 로직이다.
  - 해당 대여 품목을 찾아 연체 상태로 변경한다.
  - RentalCard 상태를 대여 불가로 변경한다.

- `makeAvailableRental`
  - 대여 품목이 남아 있으면 정지 해제를 거부한다.
  - 입력 포인트가 연체료와 같지 않으면 정지 해제를 거부한다.
  - 포인트로 연체료를 제거한다.
  - 연체료가 0이면 RentalCard 상태를 대여 가능으로 변경한다.

### 애플리케이션 유스케이스

| Use Case | Input Port | 목적 |
|---|---|---|
| `CreateRentalCardUsecase` | `CreateRentalCardInputPort` | 사용자 정보로 대여카드를 생성하고 저장한다. |
| `RentItemUsecase` | `RentItemInputPort` | 대여카드를 로딩하거나 생성한 뒤 대여 처리를 위임한다. |
| `ReturnItemUsercase` | `ReturnItemInputPort` | 대여카드를 로딩하고 반납 처리를 위임한다. |
| `OverdueItemUsercase` | `OverDueItemInputPort` | 대여 품목을 연체 처리한다. |
| `ClearOverdueItemUsecase` | `ClearOverdueItemInputPort` | 사용자 포인트로 연체를 해제한다. |
| `InquiryUsecase` | `InquiryInputPort` | 대여카드, 대여 목록, 반납 목록을 조회한다. |

원문 DTO 예시는 `UserInputDTO`, `UserItemInputDTO`, `ClearOverdueInfoDTO`, `RentalCardOutputDTO`, `RentalResultOutputDTO`, `RentItemOutputDTO`, `ReturnItemOutputDTO`를 사용한다. DTO는 유스케이스마다 달라질 수 있으며, 재사용만을 위해 큰 DTO 하나로 합치는 것은 바람직하지 않다고 설명한다.

### Output Port와 어댑터

- `RentalCardOuputPort`
  - `loadRentalCard(String userId)`
  - `save(RentalCard rentalCard)`
- JPA Adapter
  - `RentalCardJpaAdapter`가 Output Port를 구현한다.
  - `RentalCardRepository`가 `JpaRepository<RentalCard, RentalCardNo>`를 상속한다.
  - 회원 ID나 RentalCardNo로 조회한다.

### REST API

| Method | Path | 기능 |
|---|---|---|
| `POST` | `/api/RentalCard/` | 도서카드 생성 |
| `GET` | `/api/RentalCard/{id}` | 도서카드 조회 |
| `GET` | `/api/RentalCard/{id}/rentbook` | 대여 도서 목록 조회 |
| `GET` | `/api/RentalCard/{id}/returnbook` | 반납 도서 목록 조회 |
| `POST` | `/api/RentalCard/rent` | 대여 |
| `POST` | `/api/RentalCard/return` | 반납 |
| `POST` | `/api/RentalCard/overdue` | 연체 처리 |
| `POST` | `/api/RentalCard/clearoverdue` | 연체 해제 |

## 도서 마이크로서비스

### 도메인 모델

| 모델 | 유형 | 핵심 속성 |
|---|---|---|
| `Book` | Entity | 도서번호, 제목, 상세, 분류, 상태, 위치 |
| `BookDesc` | VO | 설명, 저자, ISBN, 출판일, 출처 |
| `Source` | Enum | `DONATION`, `SUPPLY` |
| `Classification` | Enum | `ARTS`, `COMPUTER`, `LITERATURE` |
| `BookStatus` | Enum | `ENTERED`, `AVAILABLE`, `UNAVAILABLE` |
| `Location` | Enum | `JEONGJA`, `PANGYO` |

### 핵심 비즈니스 로직

- `enterBook`
  - 도서 상세 정보를 생성한다.
  - 도서 제목, 상세, 분류, 위치를 설정한다.
  - 도서 상태를 `ENTERED`로 설정한다.

- `makeAvailable`
  - 도서 상태를 `AVAILABLE`로 변경한다.

- `makeUnavailable`
  - 도서 상태를 `UNAVAILABLE`로 변경하는 의도의 로직이다.
  - 원문 코드 일부에는 `AVAILABLE`로 설정하는 오탈자가 있으므로 구현 시 주의한다.

### 애플리케이션 유스케이스

| Use Case | Input Port | 목적 |
|---|---|---|
| `AddBookUsecase` | `AddBookInputPort` | 도서를 입고 상태로 생성하고 저장한다. |
| `MakeAvailableUsecase` | `MakeAvailableInputPort` | 도서를 로딩한 뒤 이용 가능 처리한다. |
| `MakeUnAvailableUsecase` | `MakeUnAvailableInputPort` | 도서를 로딩한 뒤 이용 불가 처리한다. |
| `InquiryUsecase` | `InquiryInputPort` | 도서번호로 도서 정보를 조회한다. |

원문 DTO 예시는 `BookInfoDTO`와 `BookOutPutDTO`를 사용한다. Output Port는 `BookOutPort` 또는 `BookOutPutPort`로 표현되며 도서 로딩과 저장을 담당한다.

### REST API

| Method | Path | 기능 |
|---|---|---|
| `POST` | `/api/book` | 도서 등록 |
| `GET` | `/api/book/{no}` | 도서 조회 |

## 회원 마이크로서비스

### 도메인 모델

| 모델 | 유형 | 핵심 속성 |
|---|---|---|
| `Member` | Entity | 회원번호, `IDName`, `PassWord`, `Email`, 권한 목록, `Point` |
| `IDName` | VO | 아이디, 이름 |
| `PassWord` | VO | 현재 암호, 과거 암호 |
| `Email` | VO | 이메일 주소 |
| `Authority` | VO | 사용자 역할 |
| `UserRole` | Enum | `ADMIN`, `USER` |
| `Point` | VO | 포인트 값, 적립, 사용 |

### 핵심 비즈니스 로직

- `registerMember`
  - `IDName`, `PassWord`, `Email`로 회원을 생성한다.
  - 포인트를 0으로 초기화한다.
  - 기본 권한으로 `USER`를 추가한다.

- `savePoint`
  - 회원의 포인트를 증가시킨다.

- `usePoint`
  - 회원의 포인트를 차감한다.
  - 보유 포인트보다 큰 포인트를 사용하려 하면 예외를 발생시킨다.

- `login`, `logout`
  - 원문에는 메서드 틀만 있고 실제 구현은 없다.

### 애플리케이션 유스케이스

| Use Case | Input Port | 목적 |
|---|---|---|
| `AddMemberUsecase` | `AddMemberInputPort` | 회원 객체에게 생성을 위임하고 저장한다. |
| `SavePointUsecase` | `SavePointInputPort` | 회원을 로딩한 뒤 포인트 적립을 위임한다. |
| `UsePointUsecase` | `UsePointInputPort` | 회원을 로딩한 뒤 포인트 사용을 위임한다. |
| `InquiryMemberUsecase` | `InquiryMemberInputPort` | 회원번호로 회원을 조회한다. |

원문 DTO 예시는 `MemberInfoDTO`와 `MemberOutPutDTO`를 사용한다. Output Port는 `MemberOutPutPort`이며 회원번호 조회, IDName 조회, 회원 저장 기능을 제공한다.

### REST API

| Method | Path | 기능 |
|---|---|---|
| `POST` | `/api/Member/` | 회원 등록 |
| `GET` | `/api/Member/{no}` | 회원 조회 |

## BEST 서적 마이크로서비스

### 아키텍처와 목적

원문은 BEST 서적 서비스를 지원 도메인으로 보고 레이어드 아키텍처와 CQRS 성격의 읽기 모델로 구현한다. 대여 이벤트를 소비해 MongoDB에 베스트 도서 목록을 유지한다.

원문 레이어:

- `web`: API 발행, 이벤트 컨슈머
- `domain`: 비즈니스 로직, 유스케이스, 도메인 모델
- `persistence`: 데이터 액세스

### 도메인 모델

| 모델 | 유형 | 핵심 속성 |
|---|---|---|
| `BestBook` | MongoDB Document | id, item, rentCount |
| `Item` | VO | 품목 번호, 제목 |

### 핵심 비즈니스 로직

- `registerBestBook`
  - UUID로 ID를 생성한다.
  - 전달된 품목을 설정한다.
  - 대여 횟수를 1로 설정한다.

- `increaseBestBookCount`
  - 현재 대여 횟수를 1 증가시킨다.

- `dealBestBook`
  - 품목으로 기존 베스트 도서를 조회한다.
  - 존재하면 대여 횟수를 증가시킨다.
  - 존재하지 않으면 베스트 도서로 최초 등록한다.
  - 처리 후 MongoDB에 저장한다.

### REST API

원문 컨트롤러는 DTO 없이 도메인 객체를 직접 사용한다.

| Method | Path | 기능 |
|---|---|---|
| `GET` | `/api/books` | 전체 베스트 도서 조회 |
| `GET` | `/api/books/{id}` | 베스트 도서 단건 조회 |
| `POST` | `/api/books` | 베스트 도서 생성 |
| `PUT` | `/api/books/{id}` | 베스트 도서 수정 |

`dealBestBook`은 API에서 직접 호출하지 않고, 이후 작성하는 Kafka 이벤트 소비자가 호출한다.

## EDA 구현

### 이벤트 기반 통신 개념

- 데이터를 생산하고 소유하는 것과 데이터에 접근하는 행위를 분리한다.
- 이벤트 스트림은 단일 진실 공급원으로 다룬다.
- 이벤트 브로커는 확장성, 보존성, 고가용성, 고성능을 제공해야 한다.
- 이벤트 스트림은 파티션으로 분할될 수 있다.
- 파티션 내부에서는 순서가 보장된다.
- 이벤트는 한번 발행되면 수정되지 않는 불변 데이터로 다룬다.
- 이벤트에는 오프셋이 할당되고 컨슈머는 오프셋을 기준으로 소비 위치를 관리한다.
- 이벤트 스트림은 재연 가능해야 한다.

### 도메인 이벤트

원문은 대여 마이크로서비스의 `RentalCard`가 이벤트 생성 책임을 갖는다고 설명한다.

| 이벤트 | 발생 시점 | 주요 데이터 |
|---|---|---|
| `ItemRented` | 도서 대여 완료 | `IDName`, `Item`, point |
| `ItemReturned` | 도서 반납 완료 | `IDName`, `Item`, point |
| `OverdueCleared` | 대여 정지 해제 완료 | `IDName`, point |

이벤트 설계 원칙:

- 이벤트는 하나의 목적만 가진다.
- 이벤트 크기는 최소화한다.
- 스트림별 이벤트 정의를 명확히 한다.

### 이벤트 흐름

| Producer | Event | Consumer | Consumer 처리 |
|---|---|---|---|
| rental-service | `ItemRented` | book-service | 도서를 이용 불가로 변경 |
| rental-service | `ItemRented` | member-service | 회원 포인트 10 적립 |
| rental-service | `ItemRented` | bestbook-service | 베스트 도서 추가 또는 대여 횟수 증가 |
| rental-service | `ItemReturned` | book-service | 도서를 이용 가능으로 변경 |
| rental-service | `ItemReturned` | member-service | 회원 포인트 10 적립 |
| rental-service | `OverdueCleared` | member-service | 회원 포인트 사용 |

### Kafka 토픽과 그룹

원문 설명과 예제 설정을 종합하면 다음 토픽이 사용된다.

| 토픽 | 의미 | 주요 Consumer Group |
|---|---|---|
| `rental_rent` | 도서 대여됨 | `book`, `member`, `bestbook` |
| `rental_return` | 도서 반납됨 | `book`, `member` |
| `overdue_clear` | 대여 정지 해제됨 | `member` |

원문 초반에는 개념 토픽으로 `lent`, `return`, `overdue`도 언급하지만, 실제 설정 예시는 `rental_rent`, `rental_return`, `overdue_clear`를 사용한다.

### Producer와 Consumer 구현 방식

- 대여 서비스는 `EventOutputPort`를 정의한다.
- `EventOutputPort`는 대여, 반납, 정지해제 이벤트 발행 메서드를 가진다.
- Kafka Producer Adapter가 `EventOutputPort`를 구현한다.
- Application Input Port는 도메인 로직 처리 후 이벤트를 생성하고 Output Port에 발행을 위임한다.
- Consumer는 Kafka 메시지를 역직렬화하고 애플리케이션 유스케이스를 호출한다.

원문 Consumer 책임:

- book-service Consumer
  - `rental_rent` 수신: 도서 이용 불가 처리
  - `rental_return` 수신: 도서 이용 가능 처리

- member-service Consumer
  - `rental_rent` 수신: 포인트 적립
  - `rental_return` 수신: 포인트 적립
  - `overdue_clear` 수신: 포인트 사용

- bestbook-service Consumer
  - `rental_rent` 수신: 베스트 도서 집계 반영

### EDA 통합 테스트 흐름

원문 테스트 시나리오는 다음 순서로 구성된다.

1. 회원 등록
2. 도서 A 등록 및 최초 상태 확인
3. 대여카드 등록
4. 도서 A 대여
5. 도서 A 상태가 대여 불가로 변경되는지 확인
6. 회원 포인트가 증가하는지 확인
7. 베스트 도서 정보가 생성되는지 확인
8. 도서 A 반납
9. 도서 A 상태가 대여 가능으로 변경되는지 확인
10. 회원 포인트가 다시 증가하는지 확인
11. 도서 B 등록 및 최초 상태 확인
12. 도서 B 대여
13. 도서 B 상태가 대여 불가로 변경되는지 확인
14. 회원 포인트가 증가하는지 확인
15. 베스트 도서 정보가 생성되는지 확인
16. 도서 B 연체 처리
17. 대여카드 상태가 대여 정지인지 확인
18. 연체 도서 B 반납
19. 대여 정지 해제 처리
20. 회원 포인트가 사용되어 감소하는지 확인

## EDA-SAGA 구현

### 문제 정의

원문은 단순 이벤트 발행 후 Consumer 실패가 발생했을 때의 정합성 문제를 제기한다.

- 대여 이벤트가 발행되었지만 도서 서비스가 도서 이용 불가 처리를 실패하면 어떻게 할 것인가?
- 이미 적립된 회원 포인트도 취소해야 하는가?
- 베스트 도서 카운트도 줄여야 하는가?
- 반납 이벤트가 발행되었지만 도서 서비스가 도서 이용 가능 처리를 실패하면 반납 취소가 필요한가?
- 정지해제 이벤트가 발행되었지만 회원 서비스의 포인트 사용이 실패하면 정지해제 취소가 필요한가?

### 메시징 상호작용 결정

원문은 다음 두 방식을 조합한다.

- 발행/구독: 여러 Consumer가 같은 이벤트를 소비한다.
- 비동기 요청/응답: 처리 결과가 필요한 경우 결과 메시지를 별도 채널로 돌려보낸다.

### 변경된 이벤트 흐름

추가 토픽:

| 토픽 | 목적 |
|---|---|
| `rental_result` | 도서 서비스와 회원 서비스가 처리 성공/실패 결과를 대여 서비스에 반환 |
| `point_use` | 보상 트랜잭션 후 회원 포인트 적립을 취소하기 위한 커맨드 |

변경된 규칙:

- 결과 스트림의 메시지 포맷은 통일한다.
- 결과 메시지는 이벤트 타입, 성공/실패 여부, 회원, 아이템, 포인트를 담는다.
- 도서 서비스는 대여와 반납 처리에 대해 항상 결과 메시지를 보낸다.
- 회원 서비스는 정지해제 처리에 대해서 결과 메시지를 보낸다.
- 대여 서비스는 결과 스트림을 구독한다.
- 결과가 실패이면 대여 서비스가 보상 트랜잭션을 수행한다.
- 대여 또는 반납 보상 후 이미 적립된 회원 포인트를 회수하기 위해 `point_use` 커맨드를 발행한다.
- BEST 도서 서비스는 단방향 집계로 보고 대여 취소 시 별도 보상하지 않는다.

### EventResult

원문 공통 결과 메시지:

| 필드 | 의미 |
|---|---|
| `eventType` | `RENT`, `RETURN`, `OVERDUE` |
| `isSuccessed` | 처리 성공 여부 |
| `idName` | 회원 식별 정보 |
| `item` | 대상 도서 |
| `point` | 포인트 |

원문 enum:

- `RENT`
- `RETURN`
- `OVERDUE`

### 도서 서비스 결과 응답

도서 서비스는 Consumer 처리 후 `EventResult`를 생성한다.

- `ItemRented` 처리
  - `eventType = RENT`
  - 도서 이용 불가 처리 성공 시 `isSuccessed = true`
  - 예외 발생 시 `isSuccessed = false`
  - 결과를 `rental_result`로 발행

- `ItemReturned` 처리
  - `eventType = RETURN`
  - 도서 이용 가능 처리 성공 시 `isSuccessed = true`
  - 예외 발생 시 `isSuccessed = false`
  - 결과를 `rental_result`로 발행

### 대여 서비스 보상 트랜잭션

`RentalCard`에 다음 보상 로직을 추가한다.

| 보상 로직 | 목적 |
|---|---|
| `cancelRentItem` | 대여 목록에서 대여 품목 제거 |
| `cancelReturnItem` | 반납 목록에서 반납 품목 제거 후 대여 목록으로 복원 |
| `cancelMakeAvailableRental` | 연체료를 다시 추가하고 대여 불가 상태로 변경 |

`CompensationUsecase`는 보상 트랜잭션을 관리하기 위해 다음 기능을 제공한다.

- `cancelRentItem(IDName idName, Item item)`
- `cancelReturnItem(IDName idName, Item item, long point)`
- `cancelMakeAvailableRental(IDName idName, long point)`

`CompensationInputPort`는 RentalCard를 로딩해 보상 메서드를 호출한다. 대여 취소와 반납 취소의 경우 이미 적립된 포인트를 회수하기 위해 `PointUseCommand`를 발행한다.

### PointUseCommand

| 필드 | 의미 |
|---|---|
| `idName` | 포인트를 회수할 회원 |
| `point` | 회수할 포인트 |

이 메시지는 도메인 이벤트가 아니라 회원 포인트 사용을 지시하는 커맨드 메시지로 설명된다.

### 대여 서비스 결과 Consumer

대여 서비스는 `rental_result`를 구독한다.

- `isSuccessed = true`: 추가 보상 없이 로그만 남긴다.
- `isSuccessed = false`
  - `RENT`: 대여 취소 보상 트랜잭션 수행
  - `RETURN`: 반납 취소 보상 트랜잭션 수행
  - `OVERDUE`: 정지해제 취소 보상 트랜잭션 수행

### 회원 서비스 변경

회원 서비스는 두 가지 역할을 추가한다.

- `overdue_clear` 수신 시 포인트 사용을 시도하고, 성공/실패를 `rental_result`로 응답한다.
- `point_use` 수신 시 포인트 회수 커맨드를 처리한다.

### SAGA 유형

원문은 구현된 방식이 Choreography SAGA에 가깝다고 설명한다. 중앙 오케스트레이터가 명령을 내려 워크플로를 지휘하는 Orchestration SAGA가 아니라, 각 마이크로서비스가 이벤트와 결과 메시지를 기준으로 다음 처리를 판단한다.

### SAGA 테스트 방식

- 정상 케이스로 먼저 테스트한다.
- 이후 Consumer에서 일부러 실패 결과를 보내도록 수정해 보상 트랜잭션을 검증한다.
- 테스트 대상:
  - 대여 정상 처리
  - 대여 취소 보상
  - 반납 정상 처리
  - 반납 취소 보상
  - 정지해제 정상 처리
  - 정지해제 취소 보상

## Docker 및 배포

### 원문 Dockerfile

원문 Dockerfile은 Java 11 런타임 이미지를 사용한다.

```dockerfile
FROM openjdk:11-jre-slim
ARG JAR_FILE_PATH=target/*.jar
COPY ${JAR_FILE_PATH} app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### 원문 빌드 및 실행

```bash
mvn clean package
docker build -t bestbookms:0.0.1 .
docker run --name bestbookMS -p 8083:8084 bestbookms:0.0.1
```

### 원문 docker compose 구성

- MongoDB
- ZooKeeper
- Kafka
- Kafka UI
- BestBookMS 컨테이너

원문은 컨테이너 내부에서 `localhost`가 자기 컨테이너를 의미하므로, Kafka나 MongoDB 같은 외부 연계 주소를 compose 서비스명 기준으로 변경해야 한다고 설명한다.

## 현재 프로젝트 적용 기준

PDF는 학습용 예제이므로 현재 `library-rental-eda` 프로젝트에는 다음 기준으로 보정해 적용한다.

### 기술 스택 보정

| PDF 원문 | 현재 프로젝트 기준 |
|---|---|
| Java 11 | Java 21 |
| Maven | Gradle Wrapper 8.5 |
| Spring Boot 2.7.4 | Spring Boot 3.3.7 |
| H2 중심 예제 | book/member/rental은 MariaDB, bestbook은 MongoDB |
| ZooKeeper 기반 Kafka | Kafka KRaft |
| Springfox Swagger | Spring Boot 3 계열에 맞는 문서화 방식 또는 기존 프로젝트 방식 |
| `javax.*` 가능성 | Spring Boot 3 기준 `jakarta.*` |

### 패키지 구조 보정

현재 프로젝트는 다음 구조를 우선한다.

- Inbound Port: `application/port/in`
- Outbound Port: `application/port/out`
- Application Service: `application/service`
- Application DTO: `application/dto`
- Domain Model: `domain/model`
- Domain VO: `domain/vo`
- Web Adapter: `adapter/in/web`
- Web DTO: `adapter/in/web/dto`
- Kafka Consumer: `adapter/in/messaging/consumer`
- Kafka Producer: `adapter/out/messaging`
- Persistence Adapter: `adapter/out/persistence`
- Configuration: `config`

PDF의 `framework/jpaadapter`, `framework/kafkaadapter`, `framework/web`는 현재 프로젝트에서는 `adapter/out/persistence`, `adapter/out/messaging`, `adapter/in/web`, `adapter/in/messaging/consumer`로 분리한다.

### 메시지 계약 보정

- 여러 서비스가 공유하는 이벤트, 커맨드, 결과 메시지는 `common-events`에 둔다.
- 공유 계약은 Java `record`를 우선 사용한다.
- 메시지 고유 식별자는 `eventId`를 사용한다.
- 비즈니스 흐름 식별자는 `correlationId`를 사용한다.
- 도메인 이벤트, 커맨드 메시지, 결과 이벤트를 이름과 목적에서 분리한다.
- 보상 커맨드는 새 `eventId`를 만들고 기존 `correlationId`를 유지한다.

### 현재 프로젝트에서 금지되는 확장

AGENTS.md 기준으로 다음은 구현하지 않는다.

- Outbox pattern
- DLQ, DLT, dead-letter publishing
- Distributed tracing
- Custom Kafka retry/backoff infrastructure
- SAGA orchestration code
- 직접 서비스 간 HTTP 호출

### 설계상 보정할 점

- PDF의 BEST 도서 서비스는 DTO 없이 도메인을 컨트롤러에서 직접 반환하지만, 현재 프로젝트에서는 도메인 엔티티를 Controller에서 직접 노출하지 않는다.
- PDF 예제는 도메인 모델에 JPA 애노테이션을 직접 붙이는 타협안을 설명하지만, 현재 프로젝트는 가능하면 도메인 모델과 persistence entity/document를 분리한다.
- Consumer에는 비즈니스 의사결정을 두지 않고 역직렬화, 최소 검증, 유스케이스 위임만 둔다.
- SAGA 보상 결정과 상태 변경은 Consumer가 아니라 application/domain 쪽에 둔다.
- `bestbook-service`는 MongoDB `@Document`와 `MongoRepository`를 사용한다.

## PDF 원문 주의 사항

원문에는 학습용 코드와 슬라이드 작성 과정에서 생긴 것으로 보이는 오탈자와 불일치가 있다. 그대로 구현하지 말고 의도 기준으로 보정한다.

- `Ouput`, `Retrun`, `cancle`, `setSuccessed`, `OverdueCleard`, 도서 분류 enum 표기 오류, `MogoDB` 등 오탈자가 반복된다.
- 도서 서비스의 `makeUnavailabe` 예시 코드가 `AVAILABLE`로 설정되는 부분이 있으나, 문맥상 `UNAVAILABLE`이 맞다.
- 대여 가능 체크에서 요구사항은 5권 이내인데 예시 코드는 `size() > 5` 조건을 사용한다. 6번째 대여를 막으려면 구현 시 `>= 5`를 검토해야 한다.
- `RentalCardOuputPort.loadRentalCard` 반환 타입이 설명과 코드 사이에서 `RentalCard`와 `Optional<RentalCard>`로 혼재한다.
- API 포트가 섹션별로 `8080`, `8081`, `8082`, `8083`, `8084`가 혼재한다.
- Kafka 토픽 이름이 개념 설명과 설정 예시에서 `lent/return/overdue`, `rental_rent/rental_return/overdue_clear`로 혼재한다.
- `EventResult.isSuccessed` 명명은 문법적으로 어색하므로 현재 프로젝트에서는 `success`, `succeeded`, `resultStatus` 같은 명확한 이름을 검토한다.
- PDF의 SAGA는 Choreography 방식이며, 현재 프로젝트 예외 규칙상 중앙 오케스트레이션 코드는 추가하지 않는다.
