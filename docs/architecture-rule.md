# [Architecture Rule] DDD Hexagonal + EDA + DTO 분리 통합 아키텍처 규칙

---

## ⚠️ 예시 코드 안내

본 문서에 등장하는 모든 **클래스명, 메서드명, 패키지 구조, 필드명, 토픽명, 변환 로직**은
규칙의 적용 방식을 설명하기 위한 **참고 예시(Reference Example)**이다.
실제 프로젝트에서는 도메인 용어, 팀 네이밍 컨벤션, 비즈니스 요구사항에 맞게 자유롭게 변경한다.

| 구분 | 설명 |
|------|------|
| **반드시 지켜야 할 것** | Hexagonal 계층 분리, 의존성 방향, Port/Adapter 패턴, Aggregate 경계, 계층별 DTO 분리, 변환 메서드 규칙, 동기/비동기 분리, record 사용, 금지 사항 |
| **자유롭게 변경 가능한 것** | 클래스명, 메서드명, 필드 구성, 패키지 경로, 토픽명, 구현 기술 상세 |

> 본 문서의 모든 코드 블록은 **가맹점 관리(Merchant)** 또는 **결제(Payment)** 시나리오를 가정한 참고 예시이다.
> 다른 도메인에 적용할 때는 동일한 패턴과 원칙을 따르되, 도메인 용어와 비즈니스 규칙에 맞게 구성한다.

---

## 문서 구성

| Part | 내용 | 핵심 키워드 |
|------|------|-----------|
| **Part 1** | DDD Hexagonal Architecture 지침 | 의존성 방향, Port/Adapter, Aggregate, 패키지 구조 |
| **Part 2** | 계층별 DTO 분리 및 변환 메서드 규칙 | Request/Response DTO, toCommand(), from(), record |
| **Part 3** | EDA 지침 | 동기/비동기 분리, Kafka, Domain Event, Result Event |
| **Part 4** | Admin ↔ Domain Service 통합 구현 패턴 | Controller, Service, CommandHandler, Consumer |
| **Part 5** | 금지 사항 및 코드 리뷰 체크리스트 | 전체 규칙 통합 |

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
                        │   EventPublisher 등)     │
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
│   │   │   └── web/                      #   REST API Controller
│   │   │       ├── auth/                 #     인증 관련 Controller + Request/Response DTO
│   │   │       ├── command/              #     명령(등록/수정/삭제) Controller + DTO
│   │   │       ├── merchant/             #     가맹점 조회/관리 Controller + DTO
│   │   │       ├── system/               #     시스템 설정 Controller + DTO
│   │   │       └── user/                 #     사용자 관리 Controller + DTO
│   │   │
│   │   └── out/                          # Outbound Adapter (내부 → 외부)
│   │       ├── external/                 #   외부 시스템 연동 (타 서비스 API Client 등)
│   │       │   ├── MerchantServiceClient.java
│   │       │   └── dto/                  #     외부 연동 Request/Response DTO
│   │       └── persistence/              #   DB 영속성 (JPA)
│   │           ├── entity/               #     JPA Entity (Domain Entity와 분리)
│   │           ├── repository/           #     Spring Data JPA Repository + Port 구현
│   │           └── mapper/               #     JPA Entity ↔ Domain Entity Mapper
│   │
│   ├── application/                      # ── Application 계층 ──
│   │   ├── dto/                          #   Command, Query, Result (record)
│   │   ├── port/                         #   Port 인터페이스 정의
│   │   │   ├── in/                       #     Inbound Port (Use Case Interface)
│   │   │   └── out/                      #     Outbound Port (Repository, Gateway 등)
│   │   └── service/                      #   Application Service (Use Case 구현)
│   │
│   ├── domain/                           # ── Domain 계층 (★ 순수 Java) ──
│   │   ├── model/                        #   Entity, Aggregate Root, Value Object
│   │   ├── event/                        #   Domain Event
│   │   ├── exception/                    #   Domain Exception
│   │   └── policy/                       #   Domain Policy / Specification
│   │
│   ├── infrastructure/                   # ── Infrastructure (기술 지원) ──
│   │   ├── messaging/                    #   Kafka Producer/Consumer 설정 및 구현
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

#### 패키지별 역할 상세

| 패키지 | 역할 | 포함하는 것 | 포함하지 않는 것 |
|--------|------|-----------|---------------|
| **adapter.in.web** | 외부 요청 수신 (REST) | Controller, API Request/Response DTO, Validation 어노테이션 | 비즈니스 로직, DB 접근 |
| **adapter.in.web.{기능}** | 기능별 Controller 그룹 | 해당 기능의 Controller + 해당 기능 전용 Request/Response DTO | 다른 기능의 DTO |
| **adapter.out.persistence** | DB 영속성 | JPA Entity, Spring Data Repository, Port 구현, Mapper | Domain Entity, 비즈니스 로직 |
| **adapter.out.external** | 외부/타 서비스 연동 | REST Client, gRPC Client, 연동용 DTO | 비즈니스 로직, DB 접근 |
| **application.dto** | Application 계층 DTO | Command, Query, Result (record) | API 어노테이션, JPA 어노테이션 |
| **application.port.in** | Inbound Port | Use Case Interface | 구현 클래스 |
| **application.port.out** | Outbound Port | Repository, Gateway, EventPublisher 인터페이스 | 구현 클래스 |
| **application.service** | Use Case 구현 | Application Service (@Service) | Controller, JPA Entity |
| **domain.model** | 도메인 핵심 | Entity, Aggregate Root, Value Object | 프레임워크 어노테이션 일체 |
| **domain.event** | 도메인 이벤트 | Domain Event record | Kafka 관련 설정 |
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
│   ├── in/web/
│   └── out/
│       ├── external/
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
- 상태 변경 시 Domain Event를 내부에 등록
- Domain Layer에는 프레임워크 어노테이션 없음

```java
// ※ 참고 예시 — 위치: domain/model/Merchant.java
public class Merchant {
    private MerchantId id;
    private BusinessInfo businessInfo;
    private ContactInfo contactInfo;
    private SettlementInfo settlementInfo;
    private MerchantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Merchant() {}

    public static Merchant create(MerchantId id, BusinessInfo businessInfo,
            ContactInfo contactInfo, SettlementInfo settlementInfo, MerchantType type) {
        Merchant merchant = new Merchant();
        merchant.id = id;
        merchant.businessInfo = businessInfo;
        merchant.contactInfo = contactInfo;
        merchant.settlementInfo = settlementInfo;
        merchant.status = MerchantStatus.PENDING_REVIEW;
        merchant.createdAt = LocalDateTime.now();
        merchant.registerEvent(new MerchantCreatedEvent(id.getValue(), businessInfo.getBusinessName()));
        return merchant;
    }

    public void update(ContactInfo newContact, SettlementInfo newSettlement) {
        if (this.status == MerchantStatus.TERMINATED)
            throw new MerchantTerminatedException("해지된 가맹점은 수정할 수 없습니다.");
        this.contactInfo = newContact;
        this.settlementInfo = newSettlement;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new MerchantInfoUpdatedEvent(this.id.getValue()));
    }

    public void changeStatus(MerchantStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus))
            throw new InvalidStatusTransitionException(this.status + " → " + newStatus + " 전이 불가");
        MerchantStatus oldStatus = this.status;
        this.status = newStatus;
        registerEvent(new MerchantStatusChangedEvent(this.id.getValue(), oldStatus.name(), newStatus.name()));
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
    private void registerEvent(DomainEvent event) { domainEvents.add(event); }
    public MerchantId getId() { return id; }
    public MerchantStatus getStatus() { return status; }
}
```

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
public interface DomainEvent { LocalDateTime occurredAt(); }
public record MerchantCreatedEvent(String merchantId, String businessName, LocalDateTime occurredAt) implements DomainEvent {
    public MerchantCreatedEvent(String merchantId, String businessName) { this(merchantId, businessName, LocalDateTime.now()); }
}

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

// ※ 참고 예시 — 위치: domain/exception/
public abstract class DomainException extends RuntimeException { protected DomainException(String msg) { super(msg); } }
public class MerchantNotFoundException extends DomainException { public MerchantNotFoundException(String id) { super("가맹점 없음: " + id); } }
```

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
| **Domain** | DTO 없음 | DTO 없음 | `domain/model/` |

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

```
변환 메서드 소유 규칙:
  상위→하위 (Request): 상위 DTO가 to__() 보유  → Request.toCommand() ✅
  하위→상위 (Response): 상위 DTO가 from() 보유  → Response.from(result) ✅
  외부→Domain: Infrastructure DTO가 toDomain() 보유  → GatewayResponse.toDomain() ✅
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

### 예시

| 상황 | 처리 방식 | 이유 |
|------|----------|------|
| admin-bff에서 자기 소유 ops journal 메모 수정 | 동기 (`200 OK`) | admin-bff 내부 완결 |
| admin-bff에서 merchant의 organization 생성 요청 | 비동기 (`202 Accepted`) | merchant 서비스 경계를 넘는 상태 변경 |
| merchant 내부에서 organization 상태값 변경 | 동기 (`200 OK`) | merchant 내부 완결 |
| admin-bff에서 pay의 결제 설정 변경 요청 | 비동기 (`202 Accepted`) | pay 서비스 경계를 넘는 상태 변경 |

## 10. 이벤트 흐름 방향

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
| **Command Message** | `{domain}.command.{action}` | Admin Service | Domain Service | 명령 전달 |
| **Result Event** | `{domain}.event.{result}` | Domain Service | Admin Service | 명령 처리 결과 회신 |
| **Domain Event** | `{domain}.event.{state-change}` | Domain Service | 전체 시스템 | 상태 변경 전파 |

## 11. Kafka 토픽 설계 패턴

```
[Command] {domain}.command.register / update / delete
[Result]  {domain}.event.registered / updated / command-failed
[Domain]  {domain}.event.created / info-updated / status-changed
```


## 11-1. Outbox 적용 유예 정책 (현 단계)

- Outbox 패턴은 현 단계에서 적용하지 않는다.
- Command 처리 중 검증 실패/비즈니스 예외/시스템 예외가 발생하면 예외 종료로 끝내지 않고 반드시 실패 이벤트를 발행한다.
- commandId는 요청-결과 이벤트 상관관계용 추적 메타이며, 현재 단계에서는 DB 영속 저장 키로 사용하지 않는다.
- Admin/BFF는 실패 이벤트를 수신해 실패 사유를 운영 화면에 전달한다.
- 현재 단계에서는 별도 Command 저장 테이블(Outbox/Inbox)을 사용하지 않는다.
- Outbox는 차기 단계에서 도입하며, 도입 시에도 실패 이벤트 발행 정책은 유지한다.


```java
// ※ 참고 예시
@KafkaListener(topics = "merchant.command.register", groupId = "merchant-service")
public void handleRegister(MerchantRegisterCommandMessage command) {
    try {
        registerHandler.handle(command);
    } catch (Exception e) {
        eventPublisher.publishCommandResult(
            command.commandId(),
            "merchant.event.command-failed",
            new MerchantCommandFailedEvent(command.commandId(), e.getMessage(), LocalDateTime.now())
        );
    }
}
```

---

# Part 4. Admin ↔ Domain Service 통합 구현 패턴

---

## 13. Admin Service 구현 패턴

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
    private final MerchantCommandPublisherPort commandPublisher;
    @Override
    public String register(RegisterMerchantCommand command) {
        String commandId = UUID.randomUUID().toString();
        MerchantRegisterCommandMessage message = MerchantRegisterCommandMessage.builder()
                .commandId(commandId).businessName(command.businessName())
                .businessNumber(command.businessNumber()).email(command.email())
                .merchantType(command.merchantType()).requestedBy("ADMIN").requestedAt(LocalDateTime.now()).build();
        commandPublisher.publishRegisterCommand(message);
        return commandId;
    }
}

// ※ 참고 예시: Kafka Publisher — 위치: infrastructure/messaging/
@Component @RequiredArgsConstructor
public class KafkaMerchantCommandPublisher implements MerchantCommandPublisherPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Override public void publishRegisterCommand(MerchantRegisterCommandMessage msg) {
        kafkaTemplate.send("merchant.command.register", msg.businessNumber(), msg);
    }
}

// ※ 참고 예시: Result Event Consumer — 위치: infrastructure/messaging/
@Component @RequiredArgsConstructor @Slf4j
public class MerchantEventConsumer {
    private final SseEmitterRegistry sseEmitterRegistry;
    @KafkaListener(topics = "merchant.event.registered", groupId = "admin-service")
    public void handleRegistered(MerchantRegisteredEvent event) {
        sseEmitterRegistry.send(event.commandId(), AdminNotification.success("REGISTER_COMPLETED", event.merchantId()));
    }
    @KafkaListener(topics = "merchant.event.command-failed", groupId = "admin-service")
    public void handleFailed(MerchantCommandFailedEvent event) {
        sseEmitterRegistry.send(event.commandId(), AdminNotification.failure("COMMAND_FAILED", event.reason()));
    }
}
```

## 14. Domain Service 구현 패턴

```java
// ※ 참고 예시: CommandHandler — 위치: application/service/
@Service @RequiredArgsConstructor @Slf4j
public class MerchantRegisterCommandHandler {
    private final MerchantRepository merchantRepository;
    private final MerchantEventPublisherPort eventPublisher;

    @Transactional
    public void handle(MerchantRegisterCommandMessage command) {
        try {
            if (merchantRepository.existsByBusinessNumber(command.businessNumber())) {
                publishFailure(command.commandId(), "이미 등록된 사업자번호");
                return;
            }
            Merchant merchant = Merchant.create(MerchantId.generate(),
                BusinessInfo.of(command.businessName(), command.businessNumber(), command.representativeName()),
                ContactInfo.of(command.email(), command.phoneNumber()),
                SettlementInfo.of(command.bankCode(), command.accountNumber()),
                MerchantType.valueOf(command.merchantType()));
            merchantRepository.save(merchant);
            eventPublisher.publish(merchant.pullDomainEvents());
            eventPublisher.publishCommandResult(command.commandId(), "merchant.event.registered",
                new MerchantRegisteredEvent(command.commandId(), merchant.getId().getValue(), LocalDateTime.now()));
        } catch (Exception e) {
            publishFailure(command.commandId(), e.getMessage());
        }
    }
    private void publishFailure(String commandId, String reason) {
        eventPublisher.publishCommandResult(commandId, "merchant.event.command-failed",
            new MerchantCommandFailedEvent(commandId, reason, LocalDateTime.now()));
    }
}

// ※ 참고 예시: Kafka Consumer — 위치: infrastructure/messaging/
@Component @RequiredArgsConstructor
public class MerchantCommandConsumer {
    private final MerchantRegisterCommandHandler registerHandler;
    @KafkaListener(topics = "merchant.command.register", groupId = "merchant-service")
    public void handleRegister(MerchantRegisterCommandMessage command) { registerHandler.handle(command); }
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

## 15. 공유 메시지 정의

```java
@Builder public record MerchantRegisterCommandMessage(
    String commandId, String businessName, String businessNumber, String representativeName,
    String email, String phoneNumber, String bankCode, String accountNumber, String merchantType,
    String requestedBy, LocalDateTime requestedAt) {}

public record MerchantRegisteredEvent(String commandId, String merchantId, LocalDateTime registeredAt) {}
public record MerchantCommandFailedEvent(String commandId, String reason, LocalDateTime failedAt) {}
```

---

# Part 5. 금지 사항 및 코드 리뷰 체크리스트

---

## 16. 금지 사항

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

# EDA 설계 금지 규칙

## Command / Event 흐름

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E1 | **서비스 간 상태 변경을 동기 REST로 직접 처리 금지** | 서비스 경계를 넘는 CUD는 반드시 Command → Event 흐름으로 처리. 단일 서비스 내부 완결 CUD는 예외 허용 |
| E2 | **조회를 Kafka 비동기로 처리 금지** | 조회는 즉시 응답이 필요하므로 동기 처리 |
| E3 | **Admin에서 타 서비스 소유 Domain Entity 직접 생성/수정 금지** | Admin은 Command 발행만 담당. 자기 소유 데이터(command status, ops journal 등)는 직접 관리 가능 |
| E4 | **Kafka Consumer에 비즈니스 로직 및 인프라 관심사 혼재 금지** | Consumer는 메시지 수신 + Handler 위임만 담당. 비즈니스 로직은 Handler에, 재시도/에러 핸들링/커밋 정책은 인프라 설정에 분리 |

## Command 추적

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E5 | **CommandMessage에 commandId 누락 금지** | 비동기 명령의 처리 상태 추적 불가 |
| E6 | **Result Event에 commandId 누락 금지** | Admin에서 어떤 명령의 결과인지 식별 불가 |

## Event 설계

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E7 | **Domain Event와 Result Event 혼용 금지** | Domain Event는 비즈니스 사실(`OrganizationCreated`)으로 타 서비스가 후속 행위에 반응. Result Event는 커맨드 처리 결과(`OrganizationCommandSucceeded`)로 발행자의 상태 추적 용도. 토픽도 분리하여 소비자가 목적별로 구독할 수 있도록 함 |
| E8 | **실패 시 예외만 throw 금지** | catch에서 반드시 실패 Result Event 발행. 예외만 던지면 발행자가 결과를 알 수 없음 |
| E12 | **이벤트 페이로드에 소비자 특화 필드 금지** | 발행자가 소비자를 인지하면 결합도 상승. 이벤트는 발생한 사실만 담아야 함 |
| E13 | **이벤트 스키마 하위 호환성 미보장 금지** | 필드 추가는 허용, 필드 삭제/타입 변경은 금지. 스키마 변경 시 기존 소비자가 깨지지 않아야 함 |

## 데이터 소유권

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E9 | **타 서비스 DB 테이블 직접 조회 금지** | 서비스 간 데이터 소유권 침해. 필요 시 API 호출 또는 ECST로 로컬 read model 유지 |

## 정합성 보장

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E10 | **Outbox 없이 비즈니스 로직과 이벤트 발행 분리 금지** | DB 저장 성공 + 이벤트 발행 실패 시 정합성 깨짐. 반드시 같은 트랜잭션에서 outbox 기록. 발행 완료된 outbox 레코드는 삭제 또는 발행 완료 마킹하여 무한 적재 방지 |
| E11 | **Consumer 멱등성 미보장 금지** | 이벤트 재처리(재시도, 중복 전달) 시 데이터 중복 반영 방지. 모든 Consumer에 공통 적용 |

## 장애 대응

| # | 금지 규칙 | 이유 |
|---|----------|------|
| E14 | **Consumer 재시도 실패 시 무한 재시도 금지** | 재시도 횟수 초과 시 DLT(Dead Letter Topic)로 이동. 무한 재시도는 후속 메시지 처리를 차단함 |
| E15 | **이벤트 순서 보장이 필요한 경우 파티션 키 미지정 금지** | 같은 엔티티에 대한 이벤트 순서가 중요하면 엔티티 식별자(예: orgCode)를 파티션 키로 지정하여 같은 파티션으로 라우팅 |

---

## 17. 코드 리뷰 체크리스트

### DDD Hexagonal
- [ ] domain 패키지에 프레임워크 의존성이 없는가?
- [ ] Domain Entity와 JPA Entity가 분리되어 있는가?
- [ ] 의존성 방향이 바깥(adapter) → 안쪽(domain)으로만 흐르는가?
- [ ] Outbound Port가 application/port/out에 인터페이스로 정의되는가?
- [ ] Aggregate Root 상태 변경이 자체 메서드를 통해서만 이루어지는가?
- [ ] Value Object가 record이고 자체 유효성 검증을 포함하는가?
- [ ] 상태 전이가 Enum 상태 머신을 통해 검증되는가?

### DTO 분리 및 변환 메서드
- [ ] API Request에 toCommand() / toQuery()가 있는가?
- [ ] API Response에 from(Result) 정적 팩토리가 있는가?
- [ ] Application Result에 from(Domain) 정적 팩토리가 있는가?
- [ ] 모든 DTO가 record로 선언되었는가?
- [ ] Domain Entity가 Controller 반환 타입에 노출되지 않는가?
- [ ] Command/Query/Result에 프레임워크 어노테이션이 없는가?
- [ ] 마스킹/포맷팅이 API Response에서만 처리되는가?
- [ ] GatewayRequest에 from(Domain), GatewayResponse에 toDomain()이 있는가?

### EDA
- [ ] 조회가 동기 200 OK, 등록/수정이 비동기 202 Accepted인가?
- [ ] CommandHandler에서 예외 시 실패 이벤트를 발행하는가?
- [ ] Consumer 측에서 멱등성이 보장되는가?
- [ ] Kafka Consumer가 Handler에 위임만 수행하는가?
- [ ] Domain Event와 Result Event가 분리되어 있는가?

---

## 부록: 전체 시퀀스 요약

```
[등록 — 비동기 EDA]

Admin UI → POST /admin/api/v1/{domain}
  → [adapter/in/web] Controller: request.toCommand()
  → [application/service] Service: commandId 생성 → CommandPublisherPort.publish()
  → [infrastructure/messaging] KafkaPublisher → Kafka 발행
  ── 202 Accepted ──▶ Admin UI

  ═══ Kafka(domain.command.register) ═══

Domain Service:
  → [infrastructure/messaging] Consumer → 위임
  → [application/service] CommandHandler (@Transactional)
    → [domain/model] Entity.create()
    → [adapter/out/persistence] Repository (via Port)
    → [infrastructure/messaging] EventPublisher: Domain Event + Result Event

  ═══ Kafka(domain.event.registered) ═══

Admin: [infrastructure/messaging] Consumer → SSE → Admin UI: "등록 완료"


[조회 — 동기]

Admin UI → GET /admin/api/v1/{domain}/{id}
  → [adapter/in/web] Controller: request.toQuery()
  → [application/service] QueryService → [adapter/out/external] RestClient (via Port)
  → Domain Service → Result.from(entity) → Response.from(result)
  ← 200 OK ──▶ Admin UI
```




