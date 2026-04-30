# [Architecture Rule] DDD Hexagonal + EDA + DTO 분리 통합 아키텍처 규칙

> **개정 이력**: 「이벤트 기반 아키텍처 적용 마이크로서비스 개발」(한정헌, 2023) 자료의
> 도서 대여 시스템 구현 패턴을 반영하여 다음 항목을 보강·세분화함:
> - Aggregate Root의 **이벤트 정적 팩토리 메서드** 패턴 명시 (§2-1, §11)
> - **EventOutputPort** Outbound Port 정의 패턴 (§2-3, §11)
> - **Event(사실) vs Command Message(지시)** 메시지 타입 분리 (§10, §12)
> - **통합 Result Event 포맷 + EventType enum 분기** 패턴 (§12-2)
> - **SAGA Choreography 보상 트랜잭션** 가이드 신설 (§13: Part 4 신설)
> - Consumer의 **try/setSuccessed/finally publish** 표준 골격 (§12-4)

---

## ⚠️ 예시 코드 안내

본 문서에 등장하는 모든 **클래스명, 메서드명, 패키지 구조, 필드명, 토픽명, 변환 로직**은
규칙의 적용 방식을 설명하기 위한 **참고 예시(Reference Example)**이다.
실제 프로젝트에서는 도메인 용어, 팀 네이밍 컨벤션, 비즈니스 요구사항에 맞게 자유롭게 변경한다.

| 구분 | 설명 |
|------|------|
| **반드시 지켜야 할 것** | Hexagonal 계층 분리, 의존성 방향, Port/Adapter 패턴, Aggregate 경계, 계층별 DTO 분리, 변환 메서드 규칙, 동기/비동기 분리, Event vs Command 분리, record 사용, 보상 트랜잭션 규칙, 금지 사항 |
| **자유롭게 변경 가능한 것** | 클래스명, 메서드명, 필드 구성, 패키지 경로, 토픽명, 구현 기술 상세 |

> 본 문서의 모든 코드 블록은 **가맹점 관리(Merchant)** 또는 **결제(Payment)** 시나리오를 가정한 참고 예시이다.
> 다른 도메인에 적용할 때는 동일한 패턴과 원칙을 따르되, 도메인 용어와 비즈니스 규칙에 맞게 구성한다.

---

## 문서 구성

| Part | 내용 | 핵심 키워드 |
|------|------|-----------|
| **Part 1** | DDD Hexagonal Architecture 지침 | 의존성 방향, Port/Adapter, Aggregate, 패키지 구조 |
| **Part 2** | 계층별 DTO 분리 및 변환 메서드 규칙 | Request/Response DTO, toCommand(), from(), record |
| **Part 3** | EDA 지침 | 동기/비동기 분리, Event vs Command Message, 통합 Result, EventOutputPort |
| **Part 4** | SAGA Choreography 보상 트랜잭션 지침 ⭐ NEW | EventResult, isSuccessed 분기, 보상 메서드, switch routing |
| **Part 5** | Admin ↔ Domain Service 통합 구현 패턴 | Controller, Service, CommandHandler, Consumer |
| **Part 6** | 금지 사항 및 코드 리뷰 체크리스트 | 전체 규칙 통합 |

---

# Part 1. DDD Hexagonal Architecture 지침

---

## 1. Hexagonal Architecture (Ports & Adapters) 원칙

### 1-1. 아키텍처 개요

Hexagonal Architecture는 **도메인 핵심 로직을 외부 인프라로부터 완전히 격리**하는 아키텍처이다.
PG 시스템에서 이 아키텍처를 채택하는 이유:

- 카드사/VAN 연동이 변경되어도 도메인 로직에 영향 없음
- DB 교체, 메시징 시스템 교체 시 도메인 무영향
- 도메인 로직을 순수 Java로 테스트 가능 (프레임워크 부팅 불필요)
- 비즈니스 규칙의 변경과 기술 인프라의 변경을 독립적으로 관리

```
                        ┌─────────────────────────┐
                        │     Inbound Adapter      │
                        │  (REST, gRPC, Kafka      │
                        │   Consumer, Scheduler)   │
                        └───────────┬─────────────┘
                                    │ 호출
                                    ▼
                        ┌─────────────────────────┐
                        │     Inbound Port         │
                        │  (Use Case Interface)    │
                        └───────────┬─────────────┘
                                    │ 구현
                        ┌───────────▼─────────────┐
                        │   Application Service    │
                        │  (유스케이스 오케스트레이션) │
                        └───────────┬─────────────┘
                                    │ 사용
                ┌───────────────────▼───────────────────┐
                │           Domain Layer                 │
                │  (Entity, Value Object, Domain Event,  │
                │   Policy, Outbound Port Interface)     │
                │                                        │
                │   ★ 순수 Java — 프레임워크 의존 없음 ★   │
                └───────────────────┬───────────────────┘
                                    │ 인터페이스 정의
                        ┌───────────▼─────────────┐
                        │     Outbound Port        │
                        │  (Repository, Gateway,   │
                        │   EventOutputPort 등)    │
                        └───────────┬─────────────┘
                                    │ 구현
                        ┌───────────▼─────────────┐
                        │    Outbound Adapter      │
                        │  (JPA, Kafka, REST       │
                        │   Client, Redis 등)      │
                        └─────────────────────────┘
```

### 1-2. 의존성 방향 규칙 (절대 위반 금지)

```
[의존성 방향: 항상 바깥 → 안쪽으로만 향한다]

Inbound Adapter  →  Application  →  Domain  ←  Application  ←  Outbound Adapter
                                      ↑
                                 가장 안쪽 (의존성 없음)
```

| 패키지 | 의존 가능 대상 | 의존 불가 대상 |
|--------|--------------|--------------|
| **domain** | 없음 (순수 Java) | adapter, application, infrastructure, 프레임워크 전체 |
| **application** | domain | adapter, infrastructure, 프레임워크 (Spring 등) |
| **adapter.in** (Inbound) | application, domain | adapter.out, infrastructure |
| **adapter.out** (Outbound) | application, domain | adapter.in |
| **infrastructure** | 전체 가능 (설정/기술 지원) | — |
| **config** | 전체 가능 (설정 목적) | — |

> **예외**: application에서 `@Service`, `@Transactional` 등 최소한의 Spring 어노테이션은 허용한다.

### 1-3. 프로젝트 구조 및 패키지 설계

본 프로젝트는 **멀티 서비스(Multi-Service)** 구조로 구성되며,
각 서비스는 **단일 프로젝트 내에서 패키지 기반으로 Hexagonal 계층을 분리**한다.

#### 전체 서비스 구성

```
services/
├── admin-service/           # 운영 관리 서비스 (가맹점 관리, 사용자 관리, 시스템 설정)
├── merchant-service/        # 가맹점 도메인 서비스 (가맹점 등록/수정/조회)
├── notification-service/    # 알림 서비스 (웹훅, SMS, Email 발송)
├── payment-service/         # 결제 도메인 서비스 (승인, 취소, 부분취소)
└── settlement-service/      # 정산 도메인 서비스 (매입, 정산, 대사)
```

#### 단일 서비스 내부 패키지 구조 (admin-service 기준)

> 아래 구조는 실제 프로젝트 패키지 구조에 기반한 참고 예시이다.
> 다른 서비스(merchant, payment 등)도 동일한 패키지 패턴을 따른다.

```
admin-service/
├── src/main/java/com/espay/admin/
│   │
│   ├── adapter/                          # ── Adapter 계층 ──
│   │   │
│   │   ├── in/                           # Inbound Adapter (외부 → 내부)
│   │   │   ├── web/                      #   REST API Controller
│   │   │   │   ├── auth/                 #     인증 관련 Controller + Request/Response DTO
│   │   │   │   ├── command/              #     명령(등록/수정/삭제) Controller + DTO
│   │   │   │   ├── merchant/             #     가맹점 조회/관리 Controller + DTO
│   │   │   │   ├── system/               #     시스템 설정 Controller + DTO
│   │   │   │   └── user/                 #     사용자 관리 Controller + DTO
│   │   │   └── messaging/                #   Kafka Inbound Adapter (Consumer)
│   │   │       ├── consumer/             #     @KafkaListener — 메시지 수신 + Handler 위임
│   │   │       └── message/              #     수신 메시지 record 정의
│   │   │
│   │   └── out/                          # Outbound Adapter (내부 → 외부)
│   │       ├── external/                 #   외부 시스템 연동 (타 서비스 API Client 등)
│   │       │   ├── MerchantServiceClient.java
│   │       │   └── dto/                  #     외부 연동 Request/Response DTO
│   │       ├── messaging/                #   Kafka Outbound Adapter (Producer)
│   │       │   └── KafkaEventProducer.java   #     EventOutputPort 구현체
│   │       └── persistence/              #   DB 영속성 (JPA)
│   │           ├── entity/               #     JPA Entity (Domain Entity와 분리)
│   │           ├── repository/           #     Spring Data JPA Repository + Port 구현
│   │           └── mapper/               #     JPA Entity ↔ Domain Entity Mapper
│   │
│   ├── application/                      # ── Application 계층 ──
│   │   ├── dto/                          #   Command, Query, Result (record)
│   │   ├── port/                         #   Port 인터페이스 정의
│   │   │   ├── in/                       #     Inbound Port (Use Case Interface)
│   │   │   └── out/                      #     Outbound Port (Repository, EventOutputPort 등)
│   │   └── service/                      #   Application Service (Use Case 구현)
│   │
│   ├── domain/                           # ── Domain 계층 (★ 순수 Java) ──
│   │   ├── model/                        #   Entity, Aggregate Root, Value Object
│   │   ├── event/                        #   Domain Event + Result Event + Command Message record
│   │   ├── exception/                    #   Domain Exception
│   │   └── policy/                       #   Domain Policy / Specification
│   │
│   ├── infrastructure/                   # ── Infrastructure (기술 지원) ──
│   │   ├── messaging/                    #   Kafka 설정 (Config, Serializer 등)
│   │   ├── security/                     #   보안 설정 (JWT, Filter 등)
│   │   └── common/                       #   공통 유틸, 에러 핸들러 등
│   │
│   ├── config/                           # ── 설정 ──
│   │   ├── JpaConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── WebConfig.java
│   │   └── SecurityConfig.java
│   │
│   └── AdminApplication.java            # Spring Boot 메인 클래스
│
└── src/main/resources/
    └── application.yml
```

> **변경 사항**: Kafka 관련 코드를 위치별로 명확히 분리:
> - **수신(Consumer)** 은 `adapter/in/messaging/consumer/` — Inbound Adapter
> - **발행(Producer)** 은 `adapter/out/messaging/` — Outbound Adapter (`EventOutputPort` 구현체)
> - **설정(Config, Serializer)** 만 `infrastructure/messaging/` 에 위치
> Producer가 Outbound Port 구현체이므로 `adapter/out`에 두어야 의존성 방향이 일관된다.

#### 패키지별 역할 상세

| 패키지 | 역할 | 포함하는 것 | 포함하지 않는 것 |
|--------|------|-----------|---------------|
| **adapter.in.web** | 외부 요청 수신 (REST) | Controller, API Request/Response DTO, Validation 어노테이션 | 비즈니스 로직, DB 접근 |
| **adapter.in.web.{기능}** | 기능별 Controller 그룹 | 해당 기능의 Controller + 해당 기능 전용 Request/Response DTO | 다른 기능의 DTO |
| **adapter.in.messaging.consumer** | Kafka 메시지 수신 | `@KafkaListener` Consumer — 역직렬화 + Handler 위임만 | 비즈니스 로직, 보상 로직, DB 접근 직접 호출 |
| **adapter.out.persistence** | DB 영속성 | JPA Entity, Spring Data Repository, Port 구현, Mapper | Domain Entity, 비즈니스 로직 |
| **adapter.out.external** | 외부/타 서비스 연동 | REST Client, gRPC Client, 연동용 DTO | 비즈니스 로직, DB 접근 |
| **adapter.out.messaging** | Kafka 메시지 발행 | `EventOutputPort` 구현체 (KafkaTemplate 사용), 콜백 처리 | 비즈니스 로직 결정 |
| **application.dto** | Application 계층 DTO | Command, Query, Result (record) | API 어노테이션, JPA 어노테이션 |
| **application.port.in** | Inbound Port | Use Case Interface | 구현 클래스 |
| **application.port.out** | Outbound Port | Repository, Gateway, **EventOutputPort** 인터페이스 | 구현 클래스 |
| **application.service** | Use Case 구현 | Application Service (@Service), Command Handler | Controller, JPA Entity |
| **domain.model** | 도메인 핵심 | Entity, Aggregate Root, Value Object | 프레임워크 어노테이션 일체 |
| **domain.event** | 도메인 이벤트 | Domain Event, Result Event, Command Message record | Kafka 관련 설정 |
| **domain.exception** | 도메인 예외 | Domain Exception 계층 | HTTP 상태코드, Spring 예외 |
| **domain.policy** | 도메인 정책 | 비즈니스 규칙, Specification | 인프라 의존 코드 |
| **infrastructure** | 기술 인프라 | Kafka 설정, Security, 공통 유틸 | 비즈니스 로직 |
| **config** | Spring 설정 | @Configuration 클래스들 | 비즈니스 로직 |

#### adapter.in.web 내부 DTO 배치 규칙

Inbound Adapter의 Request/Response DTO는 **해당 기능 패키지 내부에 함께 배치**한다.

```
adapter/in/web/
├── merchant/
│   ├── MerchantController.java
│   ├── MerchantRegisterRequest.java      # Request DTO (record)
│   ├── MerchantUpdateRequest.java        # Request DTO (record)
│   ├── MerchantDetailResponse.java       # Response DTO (record)
│   └── CommandAcceptedResponse.java      # 공통 비동기 응답 DTO (record)
│
├── auth/
│   ├── AuthController.java
│   ├── LoginRequest.java
│   └── LoginResponse.java
│
└── user/
    ├── UserController.java
    ├── UserCreateRequest.java
    └── UserDetailResponse.java
```

> **대안**: DTO가 많아지면 기능 패키지 내에 `dto/` 하위 패키지를 만들 수 있다.
> 팀 내에서 하나의 방식을 통일한다.

#### 서비스 간 구조 일관성

**모든 서비스는 동일한 패키지 패턴을 따른다.**

```
com.espay.{service}/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   └── messaging/consumer/
│   └── out/
│       ├── external/
│       ├── messaging/
│       └── persistence/
├── application/
│   ├── dto/
│   ├── port/in, out/
│   └── service/
├── domain/
│   ├── model/
│   ├── event/
│   ├── exception/
│   └── policy/
├── infrastructure/
├── config/
└── {Service}Application.java
```

---

## 2. Domain Layer 설계 규칙

### 2-1. Aggregate Root

**패턴 규칙**:
- Aggregate Root는 해당 Aggregate의 **유일한 진입점**
- 팩토리 메서드로 생성, 상태 변경 메서드로 수정
- **상태 변경 메서드는 그 변경을 표현하는 도메인 이벤트의 정적 팩토리 메서드를 함께 제공한다** (★ 신설)
- 상태 변경 시 Domain Event를 내부에 등록 (도메인 이벤트 큐 패턴) **또는** 호출자(Application Service)가 정적 팩토리로 생성해 EventOutputPort로 발행
- Domain Layer에는 프레임워크 어노테이션 없음

#### 도메인 이벤트 생성 책임 — 두 가지 패턴 중 택일

**패턴 A: 도메인 이벤트 큐 (pull-based)**
Aggregate가 내부적으로 이벤트를 누적하고, Application Service가 `pullDomainEvents()`로 회수해 발행.
적합한 경우: 한 트랜잭션에서 여러 이벤트가 발생할 수 있는 복잡한 도메인.

**패턴 B: 정적 팩토리 메서드 (push-based)** ⭐ 본 문서 권장
상태 변경 메서드는 상태만 바꾸고, 이벤트는 `createXxxEvent()` 정적 팩토리로 외부에서 생성·발행.
적합한 경우: 상태 변경과 이벤트가 1:1로 대응하는 단순·명료한 도메인. 코드 추적 용이.

> **두 패턴의 핵심 공통점**: 이벤트 객체의 **생성 책임은 도메인이 갖는다**.
> Application Service가 이벤트의 필드를 직접 채워 `new XxxEvent(...)` 하면 도메인 캡슐화 위반이다.

```java
// ※ 참고 예시 (패턴 B 권장) — 위치: domain/model/Merchant.java
public class Merchant {
    private MerchantId id;
    private BusinessInfo businessInfo;
    private ContactInfo contactInfo;
    private SettlementInfo settlementInfo;
    private MerchantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Merchant() {}

    // ── 생성 ──
    public static Merchant create(MerchantId id, BusinessInfo businessInfo,
            ContactInfo contactInfo, SettlementInfo settlementInfo, MerchantType type) {
        Merchant merchant = new Merchant();
        merchant.id = id;
        merchant.businessInfo = businessInfo;
        merchant.contactInfo = contactInfo;
        merchant.settlementInfo = settlementInfo;
        merchant.status = MerchantStatus.PENDING_REVIEW;
        merchant.createdAt = LocalDateTime.now();
        return merchant;
    }

    // ── 상태 변경 메서드 ──
    public void update(ContactInfo newContact, SettlementInfo newSettlement) {
        if (this.status == MerchantStatus.TERMINATED)
            throw new MerchantTerminatedException("해지된 가맹점은 수정할 수 없습니다.");
        this.contactInfo = newContact;
        this.settlementInfo = newSettlement;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(MerchantStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus))
            throw new InvalidStatusTransitionException(this.status + " → " + newStatus + " 전이 불가");
        this.status = newStatus;
    }

    // ── 도메인 이벤트 정적 팩토리 (★ 핵심: 도메인이 자기 이벤트를 만든다) ──
    public static MerchantCreatedEvent createMerchantCreatedEvent(Merchant merchant) {
        return new MerchantCreatedEvent(
            merchant.id.getValue(),
            merchant.businessInfo.getBusinessName(),
            LocalDateTime.now()
        );
    }

    public static MerchantStatusChangedEvent createStatusChangedEvent(
            MerchantId id, MerchantStatus oldStatus, MerchantStatus newStatus) {
        return new MerchantStatusChangedEvent(
            id.getValue(), oldStatus.name(), newStatus.name(), LocalDateTime.now()
        );
    }

    // ── SAGA 보상 트랜잭션 메서드 (Part 4 참조) ──
    public void cancelStatusChange(MerchantStatus revertTo) {
        // 외부 도메인 처리 실패 시 상태를 되돌리는 보상 메서드
        this.status = revertTo;
        this.updatedAt = LocalDateTime.now();
    }

    public MerchantId getId() { return id; }
    public MerchantStatus getStatus() { return status; }
    public BusinessInfo getBusinessInfo() { return businessInfo; }
}
```

#### Aggregate 내부 컬렉션이 있는 경우 (예: 주문-주문상품)

자료의 `RentalCard ─ RentalItem ─ ReturnedItem` 패턴을 일반화한 가이드:

```java
// ※ 참고 예시 — Aggregate Root가 내부 컬렉션을 보유한 경우
public class Order {
    private OrderId id;
    private List<OrderItem> orderItemList = new ArrayList<>();   // mutable Entity
    private List<ReturnedItem> returnedItemList = new ArrayList<>();
    private OrderStatus status;

    // ── 정상 처리 ──
    public Order addItem(Item item) {
        OrderItem orderItem = OrderItem.create(item);
        this.addOrderItem(orderItem);
        return this;
    }

    public Order returnItem(Item item, LocalDate returnDate) {
        OrderItem target = orderItemList.stream()
            .filter(oi -> oi.getItem().equals(item)).findFirst()
            .orElseThrow(() -> new ItemNotFoundException(item));
        this.removeOrderItem(target);
        this.addReturnedItem(ReturnedItem.from(target, returnDate));
        return this;
    }

    // ── 보상 트랜잭션 (SAGA) ──
    public Order cancelAddItem(Item item) {
        OrderItem orderItem = orderItemList.stream()
            .filter(oi -> oi.getItem().equals(item)).findFirst().get();
        this.removeOrderItem(orderItem);
        return this;
    }

    public Order cancelReturnItem(Item item) {
        ReturnedItem returnedItem = returnedItemList.stream()
            .filter(ri -> ri.getOrderItem().getItem().equals(item)).findFirst().get();
        this.addOrderItem(returnedItem.getOrderItem());
        this.removeReturnedItem(returnedItem);
        return this;
    }

    // ── 내부 캡슐화: 컬렉션 조작은 private 메서드로 ──
    private void addOrderItem(OrderItem item) { this.orderItemList.add(item); }
    private void removeOrderItem(OrderItem item) { this.orderItemList.remove(item); }
    private void addReturnedItem(ReturnedItem item) { this.returnedItemList.add(item); }
    private void removeReturnedItem(ReturnedItem item) { this.returnedItemList.remove(item); }

    // ── 도메인 이벤트 정적 팩토리 ──
    public static OrderItemAddedEvent createItemAddedEvent(Order order, OrderItem item) {
        return new OrderItemAddedEvent(order.id.getValue(), item.getItem(), LocalDateTime.now());
    }
    public static OrderItemReturnedEvent createItemReturnedEvent(Order order, ReturnedItem item) {
        return new OrderItemReturnedEvent(order.id.getValue(), item.getOrderItem().getItem(), LocalDateTime.now());
    }
}
```

> **VO vs Entity 결정 가이드** (자료 기반):
> 처음 설계 시 VO로 가정해도, **속성 중 하나라도 라이프사이클 동안 변경된다면 Entity로 재정의**한다.
> 예: `OrderItem.overdued`(연체 여부)가 변경되는 순간 OrderItem은 Entity여야 한다.

### 2-2. Value Object

```java
// ※ 참고 예시 — 위치: domain/model/
public record MerchantId(String value) {
    public MerchantId {
        if (value == null || value.isBlank()) throw new InvalidMerchantIdException("MerchantId 비어있음");
    }
    public static MerchantId generate() { return new MerchantId(UUID.randomUUID().toString()); }
    public static MerchantId of(String value) { return new MerchantId(value); }
    public String getValue() { return value; }
}

public record Money(long amount, Currency currency) {
    public Money { if (amount < 0) throw new InvalidMoneyException("금액은 0 이상"); }
    public static Money of(long amount) { return new Money(amount, Currency.KRW); }
    public Money add(Money other) { validateSameCurrency(other); return new Money(this.amount + other.amount, this.currency); }
    private void validateSameCurrency(Money other) { if (this.currency != other.currency) throw new CurrencyMismatchException("통화 불일치"); }
}
```

### 2-3. 상태 머신, Domain Event, Policy, Port, Exception

```java
// ※ 참고 예시 — 위치: domain/model/
public enum MerchantStatus {
    PENDING_REVIEW, ACTIVE, SUSPENDED, TERMINATED;
    public boolean canTransitionTo(MerchantStatus target) {
        return switch (this) {
            case PENDING_REVIEW -> target == ACTIVE || target == TERMINATED;
            case ACTIVE -> target == SUSPENDED || target == TERMINATED;
            case SUSPENDED -> target == ACTIVE || target == TERMINATED;
            case TERMINATED -> false;
        };
    }
}

// ※ 참고 예시 — 위치: domain/event/
// 모든 메시지 타입의 공통 마커 — Kafka 직렬화 가능
public sealed interface IntegrationMessage extends Serializable
    permits DomainEvent, ResultEvent, CommandMessage {}

// 1) Domain Event — "발생한 사실"
public sealed interface DomainEvent extends IntegrationMessage {
    LocalDateTime occurredAt();
}
public record MerchantCreatedEvent(
    String merchantId, String businessName, LocalDateTime occurredAt
) implements DomainEvent {}

public record MerchantStatusChangedEvent(
    String merchantId, String oldStatus, String newStatus, LocalDateTime occurredAt
) implements DomainEvent {}

// 2) Result Event — "명령 처리 결과"
public sealed interface ResultEvent extends IntegrationMessage {
    String commandId();
    boolean isSuccessed();
}
public record MerchantCommandResult(
    String commandId, EventType eventType, boolean isSuccessed,
    String merchantId, String reason, LocalDateTime occurredAt
) implements ResultEvent {}

public enum EventType { REGISTER, UPDATE, STATUS_CHANGE, TERMINATE }

// 3) Command Message — "처리해 달라는 지시" (보상 트랜잭션 트리거 포함)
public sealed interface CommandMessage extends IntegrationMessage {
    String commandId();
}
public record CancelMerchantRegisterCommand(
    String commandId, String merchantId, String reason, LocalDateTime requestedAt
) implements CommandMessage {}

// ※ 참고 예시 — 위치: domain/policy/
public class MerchantRegistrationPolicy {
    public void validate(BusinessInfo info, MerchantType type) {
        if (type == MerchantType.INDIVIDUAL && info.getRepresentativeName() == null)
            throw new PolicyViolationException("개인 가맹점은 대표자명 필수");
    }
}

// ※ 참고 예시 — 위치: application/port/in/
public interface RegisterMerchantUseCase { String register(RegisterMerchantCommand command); }
public interface GetMerchantQueryUseCase { MerchantDetailResult getDetail(MerchantDetailQuery query); }

// ※ 참고 예시 — 위치: application/port/out/
public interface MerchantRepository {
    Merchant save(Merchant merchant);
    Optional<Merchant> findById(MerchantId id);
    boolean existsByBusinessNumber(String businessNumber);
}

// ★ EventOutputPort — 도메인이 "Kafka"라는 기술을 모르고 "이벤트 발행"이라는 비즈니스 행위만 알게 함
public interface EventOutputPort {
    void publishCreatedEvent(MerchantCreatedEvent event);
    void publishStatusChangedEvent(MerchantStatusChangedEvent event);
    void publishCommandResult(MerchantCommandResult result);
    void publishCompensationCommand(CommandMessage command);
}

// ※ 참고 예시 — 위치: domain/exception/
public abstract class DomainException extends RuntimeException { protected DomainException(String msg) { super(msg); } }
public class MerchantNotFoundException extends DomainException { public MerchantNotFoundException(String id) { super("가맹점 없음: " + id); } }
```

> **Outbound Port 명명 규칙**:
> - **저장소 Port**: `XxxRepository`
> - **타 시스템 호출 Port**: `XxxGateway` 또는 `XxxClient`
> - **이벤트/메시지 발행 Port**: `EventOutputPort` (단일 통합) 또는 도메인별 `XxxEventPublisherPort`
> 본 문서는 자료의 패턴을 따라 **`EventOutputPort` 단일 통합형**을 권장한다.

---

## 3. Infrastructure Layer 설계 규칙

### 3-1. JPA Entity와 Domain Entity의 분리

```java
// ※ 참고 예시: JPA Entity — 위치: adapter/out/persistence/entity/
@Entity @Table(name = "merchant") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantJpaEntity {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 100) private String businessName;
    @Column(nullable = false, unique = true, length = 10) private String businessNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MerchantStatus status;
    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// ※ 참고 예시: Mapper — 위치: adapter/out/persistence/mapper/
@Component
public class MerchantPersistenceMapper {
    public MerchantJpaEntity toJpaEntity(Merchant domain) { /* 변환 */ }
    public Merchant toDomain(MerchantJpaEntity entity) { /* 변환 */ }
}

// ※ 참고 예시: Repository 구현 — 위치: adapter/out/persistence/repository/
@Repository @RequiredArgsConstructor
public class MerchantRepositoryImpl implements MerchantRepository {
    private final MerchantJpaRepository jpaRepository;
    private final MerchantPersistenceMapper mapper;
    @Override public Merchant save(Merchant m) { jpaRepository.save(mapper.toJpaEntity(m)); return m; }
    @Override public Optional<Merchant> findById(MerchantId id) { return jpaRepository.findById(id.getValue()).map(mapper::toDomain); }
    @Override public boolean existsByBusinessNumber(String bn) { return jpaRepository.existsByBusinessNumber(bn); }
}
```

---

# Part 2. 계층별 DTO 분리 및 변환 메서드 규칙

---

## 4. DTO 분리 원칙

| 계층 | Request DTO | Response DTO | 위치 (패키지) |
|------|------------|-------------|-------------|
| **API (Inbound Adapter)** | `Xxx**Request**` | `Xxx**Response**` | `adapter/in/web/{기능}/` |
| **Application** | `Xxx**Command**` / `Xxx**Query**` | `Xxx**Result**` | `application/dto/` |
| **Infrastructure (Outbound)** | `Xxx**GatewayRequest**` | `Xxx**GatewayResponse**` | `adapter/out/external/dto/` |
| **Messaging (Kafka)** | `Xxx**Event**` / `Xxx**CommandMessage**` / `Xxx**ResultEvent**` | (응답 없음, 비동기) | `domain/event/` |
| **Domain** | DTO 없음 | DTO 없음 | `domain/model/` |

> **메시징 DTO의 위치**: Kafka로 발행/수신되는 메시지 record는 **`domain/event/`** 에 둔다.
> 이유: 발행자와 소비자 양쪽 서비스에서 같은 스키마를 공유해야 하며, 비즈니스 의미를 담은 메시지는 도메인의 일부이기 때문.
> (※ 자료의 `ItemRented`, `OverdueCleared` 등이 도메인 모듈에 위치한 것과 동일)

## 5. Request 흐름 — 변환 메서드 규칙

```java
// ※ 참고 예시 — 위치: adapter/in/web/merchant/
public record MerchantRegisterRequest(
    @NotBlank String businessName, @NotBlank String businessNumber, @NotBlank @Email String email, @NotNull MerchantType merchantType
) {
    public RegisterMerchantCommand toCommand() {
        return new RegisterMerchantCommand(businessName, businessNumber, email, merchantType.name());
    }
}

// ※ 참고 예시 — 위치: application/dto/
public record ApprovePaymentCommand(String merchantId, String orderId, long amount, String cardNumber, String expiry, String cvc) {}

// ※ 참고 예시: Controller — 위치: adapter/in/web/payment/
@PostMapping("/approve")
public ResponseEntity<PaymentApproveResponse> approve(@Valid @RequestBody PaymentApproveRequest request) {
    ApprovePaymentCommand command = request.toCommand();
    PaymentApproveResult result = approvePaymentUseCase.approve(command);
    PaymentApproveResponse response = PaymentApproveResponse.from(result);
    return ResponseEntity.ok(response);
}
```

## 6. Response 흐름 — 변환 메서드 규칙

```java
// ※ 참고 예시 — 위치: application/dto/
public record PaymentApproveResult(
    String transactionId, PaymentStatus status, String cardNumber, long approvedAmount, String merchantName, LocalDateTime approvedAt
) {
    public static PaymentApproveResult from(Payment payment) {
        return new PaymentApproveResult(payment.getTransactionId().getValue(), payment.getStatus(),
            payment.getPaymentMethod().getCardNumber(), payment.getMoney().getAmount(),
            payment.getMerchantName(), payment.getApprovedAt());
    }
}

// ※ 참고 예시 — 위치: adapter/in/web/payment/
public record PaymentApproveResponse(
    String transactionId, String status, String maskedCardNumber, long approvedAmount, String merchantName, String approvedAt
) {
    public static PaymentApproveResponse from(PaymentApproveResult result) {
        return new PaymentApproveResponse(result.transactionId(), result.status().name(),
            maskCardNumber(result.cardNumber()), result.approvedAmount(), result.merchantName(), result.approvedAt().toString());
    }
    private static String maskCardNumber(String cn) { return cn.substring(0,4) + "-****-****-" + cn.substring(12); }
}
```

## 7. Infrastructure 변환 메서드 규칙

```java
// ※ 참고 예시 — 위치: adapter/out/external/dto/
public record NicePayApproveGatewayRequest(String mid, String oid, String amt, String cardNo, String expYear, String expMonth, String cvc) {
    public static NicePayApproveGatewayRequest from(Payment payment) { /* Domain → 외부 카드사 변환 */ }
}
public record NicePayApproveGatewayResponse(String resultCode, String resultMsg, String tid, String authDate, String authCode, String amt) {
    public PaymentGatewayResult toDomain() { /* 외부 카드사 → Domain 변환 */ }
    public boolean isSuccess() { return "0000".equals(resultCode); }
}
```

## 8. 변환 메서드 명명 규칙 요약

| 변환 방향 | 메서드 명 | 위치 | 호출 예시 |
|----------|----------|------|----------|
| Request → Command | `toCommand()` | adapter/in/web/ Request | `request.toCommand()` |
| Request → Query | `toQuery()` | adapter/in/web/ Request | `request.toQuery()` |
| Domain → Result | `from(Domain)` | application/dto/ Result | `Result.from(payment)` |
| Result → Response | `from(Result)` | adapter/in/web/ Response | `Response.from(result)` |
| Result → Response (대안) | `toResponse()` | application/dto/ Result | `result.toResponse()` |
| Domain → GatewayRequest | `from(Domain)` | adapter/out/external/dto/ | `GatewayReq.from(payment)` |
| GatewayResponse → Domain | `toDomain()` | adapter/out/external/dto/ | `gatewayRes.toDomain()` |
| **Domain → Domain Event** | `createXxxEvent(...)` | **domain/model/Aggregate** (정적) | `Merchant.createMerchantCreatedEvent(merchant)` |
| **Domain Event → ResultEvent** | `from(Event, boolean)` | domain/event/ ResultEvent | `MerchantCommandResult.success(commandId, event)` |

```
변환 메서드 소유 규칙:
  상위→하위 (Request): 상위 DTO가 to__() 보유  → Request.toCommand() ✅
  하위→상위 (Response): 상위 DTO가 from() 보유  → Response.from(result) ✅
  외부→Domain: Infrastructure DTO가 toDomain() 보유  → GatewayResponse.toDomain() ✅
  Domain→Event: Aggregate가 createXxxEvent() 정적 메서드로 보유  → Merchant.createMerchantCreatedEvent(...) ✅
```

---

# Part 3. EDA (Event-Driven Architecture) 지침

---

## 9. 동기/비동기 분리 기준

| 구분 | 통신 방식 | HTTP 상태 코드 | 이유 |
|------|----------|--------------|------|
| **조회(Query)** | 동기 (REST/gRPC) | `200 OK` | 즉시 응답 필요 |
| **단일 서비스 내부 CUD** | 동기 (REST/gRPC) | `200 OK` / `201 Created` | 서비스 경계를 넘지 않으므로 동기 처리로 충분 |
| **서비스 간 상태 변경 Command** | 비동기 (Kafka) | `202 Accepted` | 도메인 규칙 검증, 이벤트 전파 수반 |

### 판단 기준

- **서비스 경계를 넘는가?** → 비동기 Command
- **서비스 내부에서 완결되는가?** → 동기 처리
- **여러 서비스의 데이터가 함께 변경되어야 하는가?** → 비동기 Command + SAGA (Part 4)

### 예시

| 상황 | 처리 방식 | 이유 |
|------|----------|------|
| admin-bff에서 자기 소유 ops journal 메모 수정 | 동기 (`200 OK`) | admin-bff 내부 완결 |
| admin-bff에서 merchant의 organization 생성 요청 | 비동기 (`202 Accepted`) | merchant 서비스 경계를 넘는 상태 변경 |
| merchant 내부에서 organization 상태값 변경 | 동기 (`200 OK`) | merchant 내부 완결 |
| admin-bff에서 pay의 결제 설정 변경 요청 | 비동기 (`202 Accepted`) | pay 서비스 경계를 넘는 상태 변경 |
| 주문 → 재고차감 + 결제 + 적립 | **비동기 + SAGA** | 여러 서비스 데이터의 결과적 일관성 |

## 10. 메시지 타입 — Event vs Command Message ⭐ 핵심 분리

이벤트 기반 시스템에서 Kafka로 흐르는 메시지는 **3가지 타입**으로 명확히 분리한다.
혼용하면 토픽 설계, 소비자 결합도, 멱등성 처리가 모두 깨진다.

| 타입 | 의미 | 토픽 패턴 | 발행자 의도 | 소비자 행동 |
|------|------|----------|-----------|-----------|
| **Domain Event** | "**일어난 사실**" | `{domain}.event.{state-change}` | 사실 통보 (수신자 미지정) | 자유롭게 후속 비즈니스 트리거 |
| **Result Event** | "**명령 처리 결과**" | `{domain}.event.{action}-result` | 명령 발행자에게 회신 | 발행자가 commandId로 매칭하여 후속 흐름 결정 |
| **Command Message** | "**해 달라는 지시**" | `{domain}.command.{action}` | 특정 수신자에게 작업 요청 | 수신자가 처리 후 Result Event 발행 |

### 시간 표현으로 구분하는 직관적 방법

| 타입 | 시제 | 예시 메시지명 |
|------|------|-------------|
| Domain Event | 과거형 | `MerchantCreated`, `OrderPlaced`, `PaymentApproved` |
| Result Event | 결과형 | `MerchantCommandResult`, `OrderCommandSucceeded` |
| Command Message | 명령형 | `RegisterMerchant`, `CancelOrder`, `ChargePoint` |

### 예시 — 가맹점 등록 플로우

```
[Admin Service]                                              [Merchant Service]
     │                                                              │
     │── 1. RegisterMerchantCommand ──▶ [Kafka: command.register] ──▶│
     │   (Command Message: 명령형)                                   │
     │                                                              │ 처리 시도
     │                                                              ↓
     │                                                          [성공]
     │◀── 2-A. MerchantCommandResult(success=true) ─[Kafka: event.register-result]
     │   (Result Event: 결과형)                                      │
     │                                                              │
     │◀── 3. MerchantCreated ─[Kafka: event.created]─────────────────│
     │   (Domain Event: 과거형) — 베스트도서/통계/감사로그 등 다수 소비자
     │
     │                                                          [실패]
     │◀── 2-B. MerchantCommandResult(success=false, reason="...") ─[Kafka: event.register-result]
```

> **혼동 방지 규칙**:
> 1. Domain Event는 **소비자를 모른다** → 페이로드에 특정 소비자용 필드 금지
> 2. Result Event는 **commandId 필수** → 발행자가 어떤 명령에 대한 결과인지 식별
> 3. Command Message는 **명시적 수신자가 있다** → 토픽이 곧 수신자

## 11. 이벤트 흐름 방향

```
[Admin Service]                                     [Domain Service]
     │                                                    │
     │── 조회 (동기 REST/gRPC) ─────────────────────▶     │  Query 즉시 처리
     │                                                    │
     │── Command Message ──▶ [Kafka] ──────────────▶      │  Command 수신 → 도메인 로직
     │                                                    │
     │   ◀── Result Event ─── [Kafka] ◀────────────       │  처리 결과 Admin 회신
     │   ◀── Domain Event ─── [Kafka] ◀────────────       │  상태 변경 전체 전파
```

| 이벤트 종류 | 토픽 패턴 (예시) | 발행자 | 소비자 | 용도 |
|------------|----------------|--------|--------|------|
| **Command Message** | `{domain}.command.{action}` | Admin Service / 협업 서비스 | Domain Service | 명령 전달 |
| **Result Event** | `{domain}.event.{action}-result` | Domain Service | 명령 발행자 | 명령 처리 결과 회신 |
| **Domain Event** | `{domain}.event.{state-change}` | Domain Service | 전체 시스템 (다수) | 상태 변경 전파 |

## 12. EventOutputPort 표준 패턴 ⭐ NEW

### 12-1. EventOutputPort 인터페이스 (Domain → Application 경계)

도메인이 Kafka라는 기술을 모르도록 하는 핵심 추상화.
**모든 메시지 발행은 EventOutputPort를 통해서만 이루어진다.**

```java
// ※ 참고 예시 — 위치: application/port/out/EventOutputPort.java
public interface EventOutputPort {

    // ── Domain Event 발행 (사실 통보) ──
    void publishCreatedEvent(MerchantCreatedEvent event);
    void publishStatusChangedEvent(MerchantStatusChangedEvent event);

    // ── Result Event 발행 (명령 처리 결과) ──
    void publishCommandResult(MerchantCommandResult result);

    // ── Command Message 발행 (보상/협업 지시) ──
    void publishCompensationCommand(CommandMessage command);
}
```

> **명명 규칙**:
> - Domain Event 발행: `publish{XxxEvent}` 또는 자료의 `occur{Xxx}Event` 패턴
> - Result Event 발행: `publish{Xxx}CommandResult`
> - Command Message 발행: `publish{Xxx}Command`

### 12-2. 통합 Result 메시지 포맷

여러 Action(REGISTER, UPDATE, STATUS_CHANGE...)의 결과를 **하나의 Result 토픽**으로 통합하면
소비자가 단일 컨슈머로 모든 결과를 처리할 수 있다 (자료의 `EventResult` 패턴).

```java
// ※ 참고 예시 — 위치: domain/event/
public record MerchantCommandResult(
    String commandId,           // ★ 명령 추적용 ID (필수)
    EventType eventType,        // ★ 어떤 명령의 결과인지 (REGISTER/UPDATE/STATUS_CHANGE)
    boolean isSuccessed,        // ★ 성공/실패 분기점
    String merchantId,          // 처리된 엔티티 식별자
    String reason,              // 실패 시 사유 (성공 시 null)
    LocalDateTime occurredAt
) implements ResultEvent {

    // 정적 팩토리 — 성공
    public static MerchantCommandResult success(
            String commandId, EventType eventType, String merchantId) {
        return new MerchantCommandResult(commandId, eventType, true, merchantId, null, LocalDateTime.now());
    }

    // 정적 팩토리 — 실패
    public static MerchantCommandResult failure(
            String commandId, EventType eventType, String merchantId, String reason) {
        return new MerchantCommandResult(commandId, eventType, false, merchantId, reason, LocalDateTime.now());
    }
}

public enum EventType { REGISTER, UPDATE, STATUS_CHANGE, TERMINATE }
```

> **선택**: 결과 메시지를 통합할지 분리할지는 도메인 복잡도에 따라 결정한다.
> - **통합형** (자료의 `EventResult` 패턴): Action이 적고 결과 처리 로직이 유사할 때 (5개 이내)
> - **분리형**: Action별로 결과 처리 로직이 크게 다를 때

### 12-3. EventOutputPort 구현체 (Outbound Adapter)

```java
// ※ 참고 예시 — 위치: adapter/out/messaging/MerchantKafkaEventProducer.java
@Component @RequiredArgsConstructor @Slf4j
public class MerchantKafkaEventProducer implements EventOutputPort {

    @Value("${producers.topic.created}")        private String TOPIC_CREATED;
    @Value("${producers.topic.status-changed}") private String TOPIC_STATUS_CHANGED;
    @Value("${producers.topic.command-result}") private String TOPIC_COMMAND_RESULT;
    @Value("${producers.topic.compensation}")   private String TOPIC_COMPENSATION;

    private final KafkaTemplate<String, MerchantCreatedEvent> createdKafkaTemplate;
    private final KafkaTemplate<String, MerchantStatusChangedEvent> statusKafkaTemplate;
    private final KafkaTemplate<String, MerchantCommandResult> resultKafkaTemplate;
    private final KafkaTemplate<String, CommandMessage> commandKafkaTemplate;

    @Override
    public void publishCreatedEvent(MerchantCreatedEvent event) {
        // ★ 파티션 키로 엔티티 식별자 사용 — 같은 가맹점의 이벤트 순서 보장
        ListenableFuture<SendResult<String, MerchantCreatedEvent>> future =
            createdKafkaTemplate.send(TOPIC_CREATED, event.merchantId(), event);

        future.addCallback(new ListenableFutureCallback<>() {
            @Override public void onSuccess(SendResult<String, MerchantCreatedEvent> result) {
                log.info("Sent CreatedEvent merchantId={} offset={}",
                    event.merchantId(), result.getRecordMetadata().offset());
            }
            @Override public void onFailure(Throwable t) {
                // ★ 발행 실패 시 보상 트랜잭션 트리거 (Part 4 §13-5 참조)
                log.error("Failed to publish CreatedEvent merchantId={}: {}",
                    event.merchantId(), t.getMessage(), t);
                // 운영 환경: DLT로 적재하거나 Outbox 재발행 큐에 적재
            }
        });
    }

    @Override
    public void publishCommandResult(MerchantCommandResult result) {
        resultKafkaTemplate.send(TOPIC_COMMAND_RESULT, result.commandId(), result);
    }

    // 나머지 메서드 동일 패턴
}
```

### 12-4. Consumer 표준 골격 — try / setSuccessed / publish

★ **자료의 핵심 패턴**: 모든 Command Consumer는 다음 골격을 따른다.

```java
// ※ 참고 예시 — 위치: adapter/in/messaging/consumer/MerchantCommandConsumer.java
@Component @RequiredArgsConstructor @Slf4j
public class MerchantCommandConsumer {

    private final RegisterMerchantUseCase registerUseCase;
    private final EventOutputPort eventOutputPort;

    @KafkaListener(topics = "${consumer.topic.register}", groupId = "${consumer.group.id}")
    public void consumeRegister(ConsumerRecord<String, RegisterMerchantCommandMessage> record) {
        RegisterMerchantCommandMessage cmd = record.value();
        MerchantCommandResult result;

        try {
            // 1) 비즈니스 처리 위임 (UseCase가 도메인 로직 + 트랜잭션 책임)
            String merchantId = registerUseCase.register(cmd.toApplicationCommand());

            // 2) 성공 결과 메시지 구성
            result = MerchantCommandResult.success(cmd.commandId(), EventType.REGISTER, merchantId);

        } catch (DomainException e) {
            // 3-A) 도메인 규칙 위반 — 사유 명확
            log.warn("Register failed (domain): commandId={}, reason={}", cmd.commandId(), e.getMessage());
            result = MerchantCommandResult.failure(cmd.commandId(), EventType.REGISTER, null, e.getMessage());

        } catch (Exception e) {
            // 3-B) 시스템 예외 — 일반 사유
            log.error("Register failed (system): commandId={}", cmd.commandId(), e);
            result = MerchantCommandResult.failure(cmd.commandId(), EventType.REGISTER, null, "system_error");
        }

        // 4) ★ 성공이든 실패든 반드시 결과 메시지 발행 (예외만 throw 금지)
        eventOutputPort.publishCommandResult(result);
    }
}
```

> **핵심 원칙**:
> 1. **Consumer는 비즈니스 로직을 직접 수행하지 않는다** — UseCase에 위임만
> 2. **try-catch로 setSuccessed(true/false) 결정** → 결과 메시지 구성
> 3. **finally가 아니라 try와 catch 모두에서 publish** (예외 throw 시 결과 미발행 방지)
> 4. **DomainException과 Exception을 분리 catch** → 사유 명확화

## 11-1. Outbox 적용 유예 정책 (현 단계)

- Outbox 패턴은 현 단계에서 적용하지 않는다.
- Command 처리 중 검증 실패/비즈니스 예외/시스템 예외가 발생하면 예외 종료로 끝내지 않고 반드시 실패 이벤트를 발행한다 (§12-4).
- commandId는 요청-결과 이벤트 상관관계용 추적 메타이며, 현재 단계에서는 DB 영속 저장 키로 사용하지 않는다.
- Admin/BFF는 실패 이벤트를 수신해 실패 사유를 운영 화면에 전달한다.
- 현재 단계에서는 별도 Command 저장 테이블(Outbox/Inbox)을 사용하지 않는다.
- Outbox는 차기 단계에서 도입하며, 도입 시에도 실패 이벤트 발행 정책은 유지한다.

---

# Part 4. SAGA Choreography 보상 트랜잭션 지침 ⭐ 신설

> 이 Part는 자료 「섹션 7: EDA-SAGA 구현」을 기반으로 신설된 영역이다.
> 여러 서비스에 걸친 트랜잭션의 결과적 일관성을 보장하기 위해, **명시적 보상 트랜잭션**을 정의한다.

---

## 13. SAGA Choreography 패턴

### 13-1. 패턴 채택 이유

분산 트랜잭션은 2PC(Two-Phase Commit) 같은 동기 락 기반으로는 마이크로서비스에 부적합하다.
SAGA는 **로컬 트랜잭션의 연쇄와 보상**으로 결과적 일관성을 달성한다.

| 구분 | Choreography | Orchestration |
|------|-------------|---------------|
| 제어 흐름 | 각 서비스가 이벤트 보고 스스로 판단 | 중앙 오케스트레이터가 워크플로 지휘 |
| 결합도 | 낮음 (서비스 간 직접 통신 없음) | 중앙 컴포넌트와 결합 |
| 가시성 | 흐름 추적 어려움 (이벤트 추적 도구 필요) | 워크플로가 한 곳에 집중되어 가시성 높음 |
| 복잡도 | 낮음 (각자의 컨슈머만 작성) | 높음 (Orchestrator State Machine 필요) |
| 적합 | 단계가 적고(3~4단계 이하) 분기가 적은 경우 | 복잡한 워크플로, 명시적 상태 추적 필요 |

> 본 문서는 **Choreography 패턴을 기본 채택**한다 (자료의 결정과 동일).
> 단계가 5개 이상이거나 복잡한 분기가 있을 경우 Orchestration 검토.

### 13-2. SAGA 구성 원칙

1. **각 서비스는 로컬 트랜잭션만 수행**한다 — 분산 락 사용 금지
2. **각 단계의 처리 결과는 Result Event로 발행**된다 (성공·실패 모두)
3. **명령 발행자(Coordinator)는 Result Event를 구독**하고, 실패 시 보상 트랜잭션을 호출한다
4. **보상 트랜잭션은 Aggregate Root의 `cancel{Action}()` 메서드로 정의**된다
5. **보상은 의미적 역원**이다 — 물리적 rollback이 아니라 비즈니스적으로 "안 한 것처럼" 만든다

### 13-3. SAGA 단계 정의 — 가맹점 등록 시나리오

```
[정상 플로우]
1. Admin → MerchantService:    RegisterMerchant Command
2. MerchantService:             가맹점 생성 (PENDING_REVIEW) + DB 저장
3. MerchantService → Admin:     MerchantCommandResult(REGISTER, success=true)
4. MerchantService → 전체:      MerchantCreated Domain Event 발행
5. SettlementService 수신:      가맹점 계좌 검증 시도
6-A. 성공: SettlementService → MerchantService:
        SettlementVerifyResult(verified=true)
        → MerchantService: status를 ACTIVE로 변경 + StatusChanged Event 발행

[실패 플로우 (계좌 검증 실패)]
6-B. 실패: SettlementService → MerchantService:
        SettlementVerifyResult(verified=false, reason="invalid_account")
        → MerchantService: ★ 보상 트랜잭션 ★
            - merchant.cancelRegistration()  // 도메인 보상 메서드
            - status를 TERMINATED로 변경
            - MerchantRegistrationCanceledEvent 발행
        → Admin 수신: 등록 취소됨 안내
```

### 13-4. Aggregate Root의 보상 메서드 정의 규칙

★ **자료의 `cancleRentItem`, `cancleReturnItem`, `cancleMakeAvailableRental` 패턴을 일반화한 규칙**

| 정상 메서드 | 보상 메서드 명명 | 책임 |
|-----------|----------------|------|
| `register(...)` | `cancelRegistration()` | 등록을 안 한 것처럼 만듦 (TERMINATED 처리 또는 삭제 마킹) |
| `addItem(item)` | `cancelAddItem(item)` | 아이템을 추가하지 않은 상태로 |
| `returnItem(item)` | `cancelReturnItem(item)` | 반납을 취소하고 다시 빌린 상태로 |
| `clearOverdue(point)` | `cancelClearOverdue(point)` | 차감했던 포인트를 다시 더하고 정지 상태로 복귀 |
| `changeStatus(newStatus)` | `cancelStatusChange(prevStatus)` | 이전 상태로 되돌림 |

```java
// ※ 참고 예시 — 위치: domain/model/Merchant.java
public class Merchant {
    // ─── 정상 메서드 ───
    public static Merchant create(...) { /* PENDING_REVIEW로 시작 */ }
    public void changeStatus(MerchantStatus newStatus) { /* 상태 머신 검증 후 변경 */ }

    // ─── 보상 메서드 (의미적 역원) ───
    public void cancelRegistration(String reason) {
        // "등록을 안 한 것처럼" — 실제 row 삭제가 아니라 TERMINATED + 사유 마킹
        if (this.status == MerchantStatus.ACTIVE)
            throw new CannotCancelActiveMerchantException("이미 활성화된 가맹점은 cancelRegistration으로 취소 불가");
        this.status = MerchantStatus.TERMINATED;
        this.terminationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancelStatusChange(MerchantStatus prevStatus) {
        // 검증 단계 없이 직접 되돌림 — 보상은 상태 머신을 우회
        this.status = prevStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // ─── 보상 이벤트 정적 팩토리 ───
    public static MerchantRegistrationCanceledEvent createCancellationEvent(Merchant m, String reason) {
        return new MerchantRegistrationCanceledEvent(m.id.getValue(), reason, LocalDateTime.now());
    }
}
```

> **보상 메서드의 특징**:
> - 보상은 **상태 머신 검증을 우회**할 수 있다 (정상 흐름과 다르게 강제 전이 가능)
> - 보상도 **자체 도메인 이벤트를 발행**한다 (`XxxCanceledEvent`)
> - 보상 메서드는 **멱등하게 설계** — 같은 보상이 두 번 호출되어도 안전해야 함

### 13-5. SAGA Coordinator (명령 발행자) 패턴

명령 발행자(예: Admin Service 또는 SAGA를 시작한 도메인 서비스)는 결과를 구독하고
실패 시 보상을 호출하는 **Coordinator** 역할을 한다.

```java
// ※ 참고 예시 — 위치: adapter/in/messaging/consumer/MerchantSagaCoordinator.java
@Component @RequiredArgsConstructor @Slf4j
public class MerchantSagaCoordinator {

    private final CompensationUseCase compensationUseCase;
    private final SseEmitterRegistry sseRegistry;

    /**
     * 가맹점 도메인 이벤트의 결과 토픽 구독
     * — 자료의 RentalEventConsumers 패턴
     */
    @KafkaListener(topics = "${consumer.topic.command-result}", groupId = "${consumer.group.id}")
    public void onCommandResult(ConsumerRecord<String, MerchantCommandResult> record) {
        MerchantCommandResult result = record.value();

        if (result.isSuccessed()) {
            // 성공 — Admin UI에 알림만 전송
            log.info("Command succeeded: commandId={}, eventType={}",
                result.commandId(), result.eventType());
            sseRegistry.send(result.commandId(),
                AdminNotification.success(result.eventType().name() + "_COMPLETED", result.merchantId()));
            return;
        }

        // ★ 실패 — 어떤 명령의 실패인지 EventType으로 분기하여 보상 호출
        log.warn("Command failed: commandId={}, eventType={}, reason={}",
            result.commandId(), result.eventType(), result.reason());

        switch (result.eventType()) {
            case REGISTER -> {
                compensationUseCase.cancelRegistration(result.merchantId(), result.reason());
                log.info("Compensation: cancelRegistration executed for merchantId={}", result.merchantId());
            }
            case STATUS_CHANGE -> {
                compensationUseCase.cancelStatusChange(result.merchantId());
                log.info("Compensation: cancelStatusChange executed for merchantId={}", result.merchantId());
            }
            case UPDATE -> {
                compensationUseCase.cancelUpdate(result.merchantId());
                log.info("Compensation: cancelUpdate executed for merchantId={}", result.merchantId());
            }
            default -> log.warn("Unknown eventType in failed result: {}", result.eventType());
        }

        sseRegistry.send(result.commandId(),
            AdminNotification.failure("COMMAND_FAILED", result.reason()));
    }
}
```

### 13-6. CompensationUseCase 정의

보상 로직은 일반 UseCase와 동일한 위치(`application/port/in/`, `application/service/`)에 둔다.
**보상도 비즈니스 로직이므로 도메인 메서드에 위임**한다.

```java
// ※ 참고 예시 — 위치: application/port/in/CompensationUseCase.java
public interface CompensationUseCase {
    void cancelRegistration(String merchantId, String reason);
    void cancelStatusChange(String merchantId);
    void cancelUpdate(String merchantId);
}

// ※ 참고 예시 — 위치: application/service/MerchantCompensationService.java
@Service @RequiredArgsConstructor @Transactional @Slf4j
public class MerchantCompensationService implements CompensationUseCase {

    private final MerchantRepository merchantRepository;
    private final EventOutputPort eventOutputPort;

    @Override
    public void cancelRegistration(String merchantId, String reason) {
        Merchant merchant = merchantRepository.findById(MerchantId.of(merchantId))
            .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        // 1) 도메인 보상 메서드 호출
        merchant.cancelRegistration(reason);
        merchantRepository.save(merchant);

        // 2) 보상 이벤트 발행 — 다른 서비스도 알 수 있도록
        MerchantRegistrationCanceledEvent canceledEvent =
            Merchant.createCancellationEvent(merchant, reason);
        eventOutputPort.publishCancellationEvent(canceledEvent);
    }
    // 나머지 메서드 동일 패턴
}
```

### 13-7. SAGA 멱등성 보장 규칙

보상 트랜잭션은 **재시도, 메시지 중복 전달, Coordinator 재기동** 등으로 여러 번 호출될 수 있다.

| 멱등성 보장 방법 | 적용 위치 | 설명 |
|----------------|----------|------|
| **이미 보상된 상태 체크** | Aggregate 보상 메서드 | `if (this.status == TERMINATED) return;` |
| **commandId 처리 이력 테이블** | application/service | 이미 처리한 commandId는 무시 |
| **Kafka idempotent producer** | infrastructure/messaging | `enable.idempotence=true` |
| **Consumer offset 정확히 1회 커밋** | KafkaConfig | 처리 완료 후에만 커밋 |

```java
// ※ 참고 예시: 보상 메서드의 멱등성
public void cancelRegistration(String reason) {
    if (this.status == MerchantStatus.TERMINATED) {
        // 이미 취소된 상태 — 멱등하게 무시
        return;
    }
    this.status = MerchantStatus.TERMINATED;
    this.terminationReason = reason;
    this.updatedAt = LocalDateTime.now();
}
```

### 13-8. SAGA 테스트 시나리오

자료 p.120의 SAGA 테스트 패턴을 일반화한 표준 시나리오:

| # | 시나리오 | 검증 포인트 |
|---|---------|-----------|
| 1 | 정상 케이스 5종 모두 성공 | 모든 Result Event가 `isSuccessed=true`로 발행되고, Admin UI에 성공 알림 전달 |
| 2 | 도메인 서비스에서 의도적 실패 유발 | UseCase에 `throw new IllegalStateException()` 삽입 후, Result Event의 `isSuccessed=false` 확인 |
| 3 | Coordinator의 보상 호출 검증 | 로그에 "Compensation: cancel{Xxx} executed" 출력 확인 |
| 4 | DB 상태 원복 검증 | 보상 후 Aggregate 상태가 의미적으로 "안 한 것"이 됐는지 확인 |
| 5 | 보상 이벤트 재발행 검증 | `XxxCanceledEvent`가 발행되어 다른 소비자도 보상을 인지 |
| 6 | 보상 멱등성 검증 | 같은 commandId의 실패 메시지를 두 번 전달했을 때 보상이 한 번만 효과 발생 |

---

# Part 5. Admin ↔ Domain Service 통합 구현 패턴

---

## 14. Admin Service 구현 패턴

```java
// ※ 참고 예시: Controller — 위치: adapter/in/web/merchant/
@RestController @RequiredArgsConstructor @RequestMapping("/admin/api/v1/merchants")
public class MerchantAdminController {
    private final GetMerchantQueryUseCase getMerchantQueryUseCase;
    private final RegisterMerchantUseCase registerMerchantUseCase;

    @GetMapping("/{merchantId}") // [조회] 동기 — 200 OK
    public ResponseEntity<MerchantDetailResponse> getMerchant(@PathVariable String merchantId) {
        MerchantDetailQuery query = MerchantDetailRequest.of(merchantId).toQuery();
        MerchantDetailResult result = getMerchantQueryUseCase.getDetail(query);
        return ResponseEntity.ok(MerchantDetailResponse.from(result));
    }

    @PostMapping // [등록] 비동기 EDA — 202 Accepted
    public ResponseEntity<CommandAcceptedResponse> register(@Valid @RequestBody MerchantRegisterRequest request) {
        RegisterMerchantCommand command = request.toCommand();
        String commandId = registerMerchantUseCase.register(command);
        return ResponseEntity.accepted().body(CommandAcceptedResponse.of(commandId, "REGISTER_REQUESTED"));
    }
}

// ※ 참고 예시: Application Service — 위치: application/service/
@Service @RequiredArgsConstructor
public class MerchantRegisterService implements RegisterMerchantUseCase {
    private final EventOutputPort eventOutputPort;   // ★ 통합 EventOutputPort 사용
    @Override
    public String register(RegisterMerchantCommand command) {
        String commandId = UUID.randomUUID().toString();
        RegisterMerchantCommandMessage message = RegisterMerchantCommandMessage.builder()
                .commandId(commandId).businessName(command.businessName())
                .businessNumber(command.businessNumber()).email(command.email())
                .merchantType(command.merchantType()).requestedBy("ADMIN").requestedAt(LocalDateTime.now()).build();
        eventOutputPort.publishRegisterCommand(message);
        return commandId;
    }
}

// ※ 참고 예시: SAGA Coordinator — 위치: adapter/in/messaging/consumer/
//   Result Event를 구독하고, 실패 시 보상 호출 (§13-5 참조)
@Component @RequiredArgsConstructor @Slf4j
public class AdminSagaCoordinator {
    private final SseEmitterRegistry sseRegistry;

    @KafkaListener(topics = "merchant.event.command-result", groupId = "admin-service")
    public void onResult(MerchantCommandResult result) {
        if (result.isSuccessed()) {
            sseRegistry.send(result.commandId(),
                AdminNotification.success(result.eventType() + "_COMPLETED", result.merchantId()));
        } else {
            // Admin은 직접 보상 책임이 없는 경우가 많음 — 도메인 서비스가 자체 보상 처리
            // (도메인 간 협업 보상이 필요한 경우만 여기서 추가 명령 발행)
            sseRegistry.send(result.commandId(),
                AdminNotification.failure("COMMAND_FAILED", result.reason()));
        }
    }
}
```

## 15. Domain Service 구현 패턴

```java
// ※ 참고 예시: CommandHandler — 위치: application/service/
@Service @RequiredArgsConstructor @Slf4j
public class MerchantRegisterCommandHandler implements RegisterMerchantUseCase {
    private final MerchantRepository merchantRepository;
    private final EventOutputPort eventOutputPort;

    @Override
    @Transactional
    public String register(RegisterMerchantApplicationCommand command) {
        // 1) 비즈니스 검증 — 도메인 규칙 위반 시 DomainException
        if (merchantRepository.existsByBusinessNumber(command.businessNumber()))
            throw new DuplicateBusinessNumberException(command.businessNumber());

        // 2) Aggregate 생성 (도메인 메서드)
        Merchant merchant = Merchant.create(MerchantId.generate(),
            BusinessInfo.of(command.businessName(), command.businessNumber(), command.representativeName()),
            ContactInfo.of(command.email(), command.phoneNumber()),
            SettlementInfo.of(command.bankCode(), command.accountNumber()),
            MerchantType.valueOf(command.merchantType()));

        // 3) 영속화
        merchantRepository.save(merchant);

        // 4) ★ 도메인 이벤트 정적 팩토리로 생성 후 발행
        MerchantCreatedEvent createdEvent = Merchant.createMerchantCreatedEvent(merchant);
        eventOutputPort.publishCreatedEvent(createdEvent);

        return merchant.getId().getValue();
    }
}

// ※ 참고 예시: Kafka Consumer — 위치: adapter/in/messaging/consumer/
//   §12-4 표준 골격 적용
@Component @RequiredArgsConstructor @Slf4j
public class MerchantCommandConsumer {
    private final RegisterMerchantUseCase registerUseCase;
    private final EventOutputPort eventOutputPort;

    @KafkaListener(topics = "merchant.command.register", groupId = "merchant-service")
    public void consumeRegister(ConsumerRecord<String, RegisterMerchantCommandMessage> record) {
        RegisterMerchantCommandMessage cmd = record.value();
        MerchantCommandResult result;

        try {
            String merchantId = registerUseCase.register(cmd.toApplicationCommand());
            result = MerchantCommandResult.success(cmd.commandId(), EventType.REGISTER, merchantId);
        } catch (DomainException e) {
            log.warn("Domain rule violation: commandId={}, reason={}", cmd.commandId(), e.getMessage());
            result = MerchantCommandResult.failure(cmd.commandId(), EventType.REGISTER, null, e.getMessage());
        } catch (Exception e) {
            log.error("System error: commandId={}", cmd.commandId(), e);
            result = MerchantCommandResult.failure(cmd.commandId(), EventType.REGISTER, null, "system_error");
        }

        // ★ 성공·실패 모두 결과 발행
        eventOutputPort.publishCommandResult(result);
    }
}

// ※ 참고 예시: 조회 Internal API — 위치: adapter/in/web/merchant/
@RestController @RequiredArgsConstructor @RequestMapping("/internal/api/v1/merchants")
public class MerchantInternalController {
    private final MerchantQueryUseCase queryUseCase;
    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantQueryResponse> findById(@PathVariable String merchantId) {
        MerchantDetailResult result = queryUseCase.findById(new MerchantDetailQuery(merchantId));
        return ResponseEntity.ok(MerchantQueryResponse.from(result));
    }
}
```

## 16. 공유 메시지 정의

```java
// ※ 참고 예시 — 위치: domain/event/

// ── Command Message ──
@Builder public record RegisterMerchantCommandMessage(
    String commandId, String businessName, String businessNumber, String representativeName,
    String email, String phoneNumber, String bankCode, String accountNumber, String merchantType,
    String requestedBy, LocalDateTime requestedAt
) implements CommandMessage {
    public RegisterMerchantApplicationCommand toApplicationCommand() {
        return new RegisterMerchantApplicationCommand(
            businessName, businessNumber, representativeName, email, phoneNumber,
            bankCode, accountNumber, merchantType
        );
    }
}

// ── Domain Event ──
public record MerchantCreatedEvent(
    String merchantId, String businessName, LocalDateTime occurredAt
) implements DomainEvent {}

public record MerchantStatusChangedEvent(
    String merchantId, String oldStatus, String newStatus, LocalDateTime occurredAt
) implements DomainEvent {}

public record MerchantRegistrationCanceledEvent(
    String merchantId, String reason, LocalDateTime occurredAt
) implements DomainEvent {}

// ── Result Event (통합 포맷) ──
public record MerchantCommandResult(
    String commandId, EventType eventType, boolean isSuccessed,
    String merchantId, String reason, LocalDateTime occurredAt
) implements ResultEvent {
    public static MerchantCommandResult success(String commandId, EventType eventType, String merchantId) {
        return new MerchantCommandResult(commandId, eventType, true, merchantId, null, LocalDateTime.now());
    }
    public static MerchantCommandResult failure(String commandId, EventType eventType, String merchantId, String reason) {
        return new MerchantCommandResult(commandId, eventType, false, merchantId, reason, LocalDateTime.now());
    }
}
```

---

# Part 6. 금지 사항 및 코드 리뷰 체크리스트

---

## 17. 금지 사항

### DDD Hexagonal 관련

| # | 금지 규칙 | 이유 |
|---|----------|------|
| H1 | **domain 패키지에 프레임워크 의존성 금지** | 순수 Java만 |
| H2 | **Domain Entity에 JPA 어노테이션 금지** | adapter/out/persistence/entity에만 |
| H3 | **domain이 adapter를 직접 참조 금지** | application/port/out 인터페이스를 통해 접근 |
| H4 | **adapter.out이 adapter.in을 참조 금지** | 의존성 방향 위반 |
| H5 | **Aggregate 외부에서 내부 Entity/VO 직접 수정 금지** | Aggregate Root 메서드 통해서만 |
| H6 | **Domain Entity에 toResult(), toResponse() 금지** | Domain이 상위 계층 알면 안 됨 |
| H7 | **Application Service에서 비즈니스 로직 직접 구현 금지** | 오케스트레이션만, 규칙은 Domain에 위임 |
| H8 | **Domain Entity를 Controller에서 직접 반환 금지** | API 스펙 ↔ 도메인 결합 방지 |
| H9 | **프레임워크 예외를 domain에서 사용 금지** | Domain Exception 별도 정의 |
| H10 | **Application Service에서 도메인 이벤트 객체를 `new`로 직접 생성 금지** ⭐ NEW | Aggregate의 `createXxxEvent()` 정적 팩토리만 사용 — 도메인 캡슐화 |
| H11 | **Aggregate에 정상 메서드만 있고 보상 메서드(`cancel{Action}`)가 없으면 SAGA 도메인이 아니거나 설계 누락** ⭐ NEW | 외부와 협업하는 상태 변경에는 반드시 의미적 역원 메서드 필요 |

### 계층별 DTO 분리 관련

| # | 금지 규칙 | 이유 |
|---|----------|------|
| D1 | **모든 DTO에 class 사용 금지 — 반드시 record** | 불변성 보장 |
| D2 | **Command/Query/Result에 프레임워크 어노테이션 금지** | adapter/in/web에만 |
| D3 | **adapter/out DTO를 application/adapter.in에서 직접 사용 금지** | 외부 스펙 전파 방지 |
| D4 | **Command가 fromRequest() 변환 메서드 보유 금지** | 하위가 상위 알면 안 됨 |
| D5 | **Controller에서 Domain Entity 직접 반환 금지** | API 스펙 ↔ 도메인 결합 |
| D6 | **Application Service에서 API Response 직접 생성 금지** | Adapter 종속 |
| D7 | **Response DTO에 비즈니스 로직 금지** | 변환/포맷팅만 |
| D8 | **Kafka 메시지 record를 application/dto에 두기 금지** ⭐ NEW | `domain/event/`에 위치 — 발행자·소비자 양 서비스가 공유하는 도메인 메시지 |

## 18. EDA 설계 금지 규칙

### Command / Event 흐름

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E1 | **서비스 간 상태 변경을 동기 REST로 직접 처리 금지** | 서비스 경계를 넘는 CUD는 반드시 Command Message → Result Event 흐름으로 처리. 단일 서비스 내부 완결 CUD는 예외 허용 |
| E2 | **조회를 Kafka 비동기로 처리 금지** | 조회는 즉시 응답이 필요하므로 동기 처리 |
| E3 | **Admin에서 타 서비스 소유 Domain Entity 직접 생성/수정 금지** | Admin은 Command 발행만 담당. 자기 소유 데이터(command status, ops journal 등)는 직접 관리 가능 |
| E4 | **Kafka Consumer에 비즈니스 로직 및 인프라 관심사 혼재 금지** | Consumer는 메시지 수신 + Handler 위임만 담당. 비즈니스 로직은 UseCase에, 재시도/에러 핸들링/커밋 정책은 인프라 설정에 분리 |
| E16 | **Domain Event / Result Event / Command Message 명명 혼용 금지** ⭐ NEW | 과거형(Event) / 결과형(Result) / 명령형(Command Message)으로 시제와 의미를 명확히 구분 |
| E17 | **EventOutputPort 외 경로로 Kafka 발행 금지** ⭐ NEW | KafkaTemplate을 도메인·애플리케이션이 직접 호출하면 헥사고널 위반. 반드시 Outbound Port 경유 |

### Command 추적

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E5 | **CommandMessage에 commandId 누락 금지** | 비동기 명령의 처리 상태 추적 불가 |
| E6 | **Result Event에 commandId 누락 금지** | Admin에서 어떤 명령의 결과인지 식별 불가 |

### Event 설계

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E7 | **Domain Event와 Result Event 혼용 금지** | Domain Event는 비즈니스 사실(`MerchantCreated`)으로 타 서비스가 후속 행위에 반응. Result Event는 커맨드 처리 결과(`MerchantCommandResult`)로 발행자의 상태 추적 용도. 토픽도 분리하여 소비자가 목적별로 구독할 수 있도록 함 |
| E8 | **실패 시 예외만 throw 금지** | catch에서 반드시 실패 Result Event 발행. 예외만 던지면 발행자가 결과를 알 수 없음. §12-4 표준 골격 준수 |
| E12 | **이벤트 페이로드에 소비자 특화 필드 금지** | 발행자가 소비자를 인지하면 결합도 상승. 이벤트는 발생한 사실만 담아야 함 |
| E13 | **이벤트 스키마 하위 호환성 미보장 금지** | 필드 추가는 허용, 필드 삭제/타입 변경은 금지. 스키마 변경 시 기존 소비자가 깨지지 않아야 함 |
| E18 | **EventType 누락된 통합 Result Event 금지** ⭐ NEW | 통합 Result 토픽을 사용한다면 어떤 명령의 결과인지 EventType enum으로 분기 가능해야 함 |

### 데이터 소유권

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E9 | **타 서비스 DB 테이블 직접 조회 금지** | 서비스 간 데이터 소유권 침해. 필요 시 API 호출 또는 ECST로 로컬 read model 유지 |

### 정합성 보장

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E10 | **Outbox 없이 비즈니스 로직과 이벤트 발행 분리 금지** | DB 저장 성공 + 이벤트 발행 실패 시 정합성 깨짐. 반드시 같은 트랜잭션에서 outbox 기록. 발행 완료된 outbox 레코드는 삭제 또는 발행 완료 마킹하여 무한 적재 방지 |
| E11 | **Consumer 멱등성 미보장 금지** | 이벤트 재처리(재시도, 중복 전달) 시 데이터 중복 반영 방지. 모든 Consumer에 공통 적용 |

### 장애 대응

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E14 | **Consumer 재시도 실패 시 무한 재시도 금지** | 재시도 횟수 초과 시 DLT(Dead Letter Topic)로 이동. 무한 재시도는 후속 메시지 처리를 차단함 |
| E15 | **이벤트 순서 보장이 필요한 경우 파티션 키 미지정 금지** | 같은 엔티티에 대한 이벤트 순서가 중요하면 엔티티 식별자(예: merchantId)를 파티션 키로 지정하여 같은 파티션으로 라우팅 |

## 19. SAGA 설계 금지 규칙 ⭐ NEW

| # | 금지 규칙 | 이유 |
|---|----------|------|
| S1 | **보상 메서드를 Application Service에 작성 금지** | 보상도 비즈니스 규칙이므로 Aggregate 내부에 `cancel{Action}()`로 정의 |
| S2 | **물리적 rollback을 SAGA 보상으로 사용 금지** | 분산 환경에서 외부 서비스의 변경은 rollback 불가. 반드시 의미적 역원 메서드로 보상 |
| S3 | **보상 메서드의 멱등성 미보장 금지** | 메시지 중복 전달 시 보상이 두 번 적용되면 데이터 손상. 상태 체크 또는 commandId 중복 처리 |
| S4 | **SAGA Coordinator의 EventType 분기 누락 금지** | 통합 Result 토픽에서 어떤 명령의 실패인지 모르면 보상 호출 불가 |
| S5 | **보상 트랜잭션의 결과 이벤트 미발행 금지** | 다른 소비자도 "이 가맹점은 결국 취소됐다"는 사실을 알아야 read model 갱신 가능 |
| S6 | **5단계 이상 SAGA를 Choreography로 구현 금지** | 흐름 추적 불가. 복잡한 워크플로는 Orchestration으로 전환 검토 |
| S7 | **보상 메서드에서 정상 메서드의 상태 머신 검증 그대로 사용 금지** | 보상은 비정상 상태에서도 호출되므로 상태 머신을 우회할 수 있어야 함 |

---

## 20. 코드 리뷰 체크리스트

### DDD Hexagonal
- [ ] domain 패키지에 프레임워크 의존성이 없는가?
- [ ] Domain Entity와 JPA Entity가 분리되어 있는가?
- [ ] 의존성 방향이 바깥(adapter) → 안쪽(domain)으로만 흐르는가?
- [ ] Outbound Port가 application/port/out에 인터페이스로 정의되는가?
- [ ] Aggregate Root 상태 변경이 자체 메서드를 통해서만 이루어지는가?
- [ ] Value Object가 record이고 자체 유효성 검증을 포함하는가?
- [ ] 상태 전이가 Enum 상태 머신을 통해 검증되는가?
- [ ] **도메인 이벤트가 Aggregate의 정적 팩토리(`createXxxEvent`)로 생성되는가?** ⭐
- [ ] **EventOutputPort 인터페이스가 application/port/out에 정의되어 있는가?** ⭐

### DTO 분리 및 변환 메서드
- [ ] API Request에 toCommand() / toQuery()가 있는가?
- [ ] API Response에 from(Result) 정적 팩토리가 있는가?
- [ ] Application Result에 from(Domain) 정적 팩토리가 있는가?
- [ ] 모든 DTO가 record로 선언되었는가?
- [ ] Domain Entity가 Controller 반환 타입에 노출되지 않는가?
- [ ] Command/Query/Result에 프레임워크 어노테이션이 없는가?
- [ ] 마스킹/포맷팅이 API Response에서만 처리되는가?
- [ ] GatewayRequest에 from(Domain), GatewayResponse에 toDomain()이 있는가?
- [ ] **Kafka 메시지 record가 domain/event/에 있는가?** ⭐

### EDA
- [ ] 조회가 동기 200 OK, 등록/수정이 비동기 202 Accepted인가?
- [ ] CommandHandler에서 예외 시 실패 이벤트를 발행하는가? (try-catch-publish 골격)
- [ ] Consumer 측에서 멱등성이 보장되는가?
- [ ] Kafka Consumer가 UseCase에 위임만 수행하는가? (비즈니스 로직 직접 작성 금지)
- [ ] Domain Event / Result Event / Command Message가 분리되어 있는가? ⭐
- [ ] **모든 메시지명이 시제(과거형/결과형/명령형)에 부합하는가?** ⭐
- [ ] **EventOutputPort를 통하지 않고 KafkaTemplate을 직접 호출하는 코드가 없는가?** ⭐
- [ ] **commandId가 모든 Command Message와 Result Event에 포함되는가?**
- [ ] **이벤트 순서 보장이 필요한 경우 파티션 키가 지정되어 있는가?**

### SAGA ⭐ NEW
- [ ] **외부 서비스와 협업하는 상태 변경에 대해 Aggregate에 보상 메서드(`cancel{Action}`)가 정의되어 있는가?**
- [ ] **SAGA Coordinator가 Result Event의 `isSuccessed`로 분기하는가?**
- [ ] **Coordinator가 EventType으로 어떤 보상을 호출할지 분기하는가?**
- [ ] **보상 메서드가 멱등하게 작성되었는가? (이미 보상된 상태 체크)**
- [ ] **보상 후 결과 이벤트(`XxxCanceledEvent`)가 발행되는가?**
- [ ] **5단계 이상의 복잡한 SAGA가 아닌가? (그렇다면 Orchestration 검토)**

---

## 부록: 전체 시퀀스 요약

```
[등록 — 비동기 EDA + SAGA Choreography]

Admin UI → POST /admin/api/v1/{domain}
  → [adapter/in/web] Controller: request.toCommand()
  → [application/service] Service: commandId 생성
  → [application/port/out] EventOutputPort.publishRegisterCommand()
  → [adapter/out/messaging] KafkaEventProducer → Kafka 발행
  ── 202 Accepted ──▶ Admin UI

  ═══ Kafka(domain.command.register) ═══

Domain Service:
  → [adapter/in/messaging/consumer] CommandConsumer
    try {
      → [application/service] CommandHandler (@Transactional)
        → [domain/model] Aggregate.create() / 비즈니스 메서드
        → [domain/model] Aggregate.createXxxEvent()  ★ 도메인이 이벤트 생성
        → [adapter/out/persistence] Repository (via Port)
        → [application/port/out] EventOutputPort.publishCreatedEvent()
        → [application/port/out] EventOutputPort.publishCommandResult(success)
    } catch (DomainException) {
        → EventOutputPort.publishCommandResult(failure)
    } catch (Exception) {
        → EventOutputPort.publishCommandResult(failure, "system_error")
    }

  ═══ Kafka(domain.event.created) ═══     ─→ 다수 소비자 (read model, 통계 등)
  ═══ Kafka(domain.event.command-result) ═══

Admin Service:
  → [adapter/in/messaging/consumer] SagaCoordinator
    if (result.isSuccessed())  → SSE: "REGISTER_COMPLETED"
    else                        → SSE: "COMMAND_FAILED" + reason

[실패 시 — SAGA Choreography 보상]
  외부 서비스(예: Settlement)가 후속 검증 실패 → SettlementVerifyResult(success=false) 발행

Domain Service:
  → SagaCoordinator (Settlement 결과 수신)
    switch (eventType) {
      case STATUS_CHANGE → compensationUseCase.cancelStatusChange()
        → [domain/model] aggregate.cancelStatusChange(prevStatus)  ★ 보상 메서드
        → [adapter/out/persistence] save
        → [application/port/out] publishCancellationEvent()
    }


[조회 — 동기]

Admin UI → GET /admin/api/v1/{domain}/{id}
  → [adapter/in/web] Controller: request.toQuery()
  → [application/service] QueryService → [adapter/out/external] RestClient (via Port)
  → Domain Service → Result.from(entity) → Response.from(result)
  ← 200 OK ──▶ Admin UI
```
