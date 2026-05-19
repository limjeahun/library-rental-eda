---
name: apply-domain-aggregate-root-rules
description: library-rental-eda에서 AGENTS.md의 Domain Aggregate Root Rules를 적용하거나 리뷰할 때 사용한다. Use when refactoring or reviewing domain aggregate roots such as RentalCard, Member, Book, BestBook, or RentalSagaState for factory-only creation, private constructors, explicit non-Lombok accessors, aggregate behavior-only mutation, validate-before-mutate rules, aggregate-confirmed domain event payloads, pullDomainEvents clearing, defensive collection exposure, and absence of Spring/JPA/Kafka/common-events/Lombok dependencies. Coordinate with apply-aggregate-domain-events-refactoring when member-service or book-service aggregate-collected domain events are involved. Do not use for unrelated UI work, direct service HTTP calls, Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, or SAGA orchestration code.
---

# Domain Aggregate Root Rules 적용

## 목적

`library-rental-eda`의 도메인 aggregate root를 `AGENTS.md`의 `Domain Aggregate Root Rules`에 맞춘다.

이 스킬은 aggregate가 상태 변경과 도메인 이벤트의 source of truth가 되도록 정리한다. 대표 기준은
`rental-service/src/main/java/com/example/library/rental/domain/model/RentalCard.java`이다.

## 필수 기준

1. `AGENTS.md`를 최우선 규칙으로 읽는다.
2. 넓은 점검이면 `.agents/skills/architecture-super-agent/scripts/Invoke-ArchitectureScan.ps1 -Root .`를 먼저 실행한다.
3. aggregate-collected domain event를 추가하거나 변경하면 `apply-aggregate-domain-events-refactoring`도 함께 따른다.
4. package boundary가 애매할 때만 `docs/architecture-rule-eda.md`를 참고한다.
5. 프로젝트 예외를 새로 도입하지 않는다: Outbox, DLQ/DLT, distributed tracing, custom Kafka retry/backoff, SAGA orchestration, direct service HTTP calls.

## Aggregate 후보

우선 다음 도메인 모델을 aggregate root 또는 aggregate-like model로 점검한다.

- `rental-service/.../domain/model/RentalCard.java`
- `member-service/.../domain/model/Member.java`
- `book-service/.../domain/model/Book.java`
- `bestbook-service/.../domain/model/BestBook.java`
- `rental-service/.../domain/model/saga/RentalSagaState.java`

`BestBook`은 event-maintained read model이다. aggregate-collected domain event를 강제하지 말고, 생성 경로, Lombok 제거, explicit accessor, defensive exposure 중심으로 정리한다.

`RentalSagaState`는 saga 추적 상태 모델이다. `correlationId`나 `sourceEventId`는 이 모델의 추적 상태일 수 있으므로, domain event metadata 금지 규칙과 기계적으로 혼동하지 않는다.

## 핵심 규칙

- 생성 경로는 intent-revealing factory로 제한한다. 신규 생성은 `create...`, `register...`, `start...` 같은 factory를 사용하고, 저장소 복원은 `reconstitute(...)`로 분리한다.
- aggregate root에는 arbitrary state를 만들 수 있는 public constructor를 두지 않는다. 생성자는 `private` 또는 필요한 최소 가시성으로 제한한다.
- 상태 변경은 aggregate behavior method로만 수행한다. setter를 추가하지 않는다.
- 상태 변경 전 aggregate 내부에서 domain invariant를 검증한다.
- service-local domain event는 실제 상태 변경 후 등록한다. 요청 수신 자체를 domain event로 만들지 않는다.
- event payload는 caller input보다 aggregate가 확정한 내부 snapshot을 우선한다. 예: `item` 파라미터 대신 `rentItem.item()` 또는 `returnItem.item().item()`.
- domain event는 Kafka metadata, topic, participant, saga step, Avro/wire concern, `common-events`에 의존하지 않는다.
- `reconstitute(...)`는 저장 상태 복원만 수행하고 domain event를 등록하지 않는다.
- `pullDomainEvents()`는 `List.copyOf(...)` 등 defensive copy를 반환한 뒤 내부 event buffer를 clear한다.
- 내부 mutable collection을 직접 노출하지 않는다. `List.copyOf(...)` 또는 동등한 불변 view를 반환한다.
- aggregate root는 Spring, JPA, Kafka, Redis, MongoDB annotation, `common-events`, Lombok에 의존하지 않는다.
- aggregate root는 Lombok accessor를 쓰지 않는다. `@Getter`, `@Setter`, `@Data`, `@Builder`를 제거하고 필요한 accessor를 직접 작성한다.
- explicit accessor는 record canonical style을 선호한다. 예: `member()`, `idName()`, `point()`, `bookStatus()`.
- 기존 API나 mapper 호환 때문에 `getRentItemList()` 같은 explicit getter를 유지할 수 있지만, Lombok 생성 메서드는 금지하고 내부 캡슐화를 지켜야 한다.

## 점검 검색

먼저 aggregate root 후보의 Lombok, public constructor, JavaBean getter, mutable collection 노출을 찾는다.

```powershell
rg -n "@Getter|@Setter|@Data|@Builder|lombok" *-service/src/main/java/**/domain/model -g "*.java"
rg -n "public [A-Z][A-Za-z0-9_]*\\(" *-service/src/main/java/**/domain/model -g "*.java"
rg -n "get[A-Z][A-Za-z0-9_]*\\(" *-service/src/main/java/**/domain/model -g "*.java"
rg -n "return .*List;|Collections\\.unmodifiableList" *-service/src/main/java/**/domain/model -g "*.java"
```

PowerShell glob이 불편하면 서비스별로 실행한다.

```powershell
rg -n "@Getter|@Setter|@Data|@Builder|lombok|public [A-Z][A-Za-z0-9_]*\\(|get[A-Z][A-Za-z0-9_]*\\(" member-service/src/main/java book-service/src/main/java rental-service/src/main/java bestbook-service/src/main/java -g "*.java"
```

## 적용 순서

한 번에 하나의 aggregate slice를 끝낸다.

### 1. Member

대상:

- `member-service/.../domain/model/Member.java`
- `MemberPersistenceMapper`
- `MemberResult`
- 관련 tests

변경:

- `@Getter`, `@Getter(AccessLevel.NONE)`와 Lombok import를 제거한다.
- public constructor를 `private`로 변경한다.
- `registerMember(...)`와 `reconstitute(...)`만 생성 경로로 유지한다.
- 생성자에서 `authorities`를 `new ArrayList<>(authorities)`로 복사한다.
- explicit accessor를 추가한다.

권장 accessor:

```java
public Long memberNo() { return memberNo; }
public MemberIdentity idName() { return idName; }
public PassWord password() { return password; }
public Email email() { return email; }
public Point point() { return point; }
public List<Authority> authorities() { return List.copyOf(authorities); }
```

호출부:

- `getMemberNo()` -> `memberNo()`
- `getIdName()` -> `idName()`
- `getPassword()` -> `password()`
- `getEmail()` -> `email()`
- `getPoint()` -> `point()`
- `getAuthorities()` -> `authorities()`

### 2. Book

대상:

- `book-service/.../domain/model/Book.java`
- `BookPersistenceMapper`
- `BookResult`
- related tests

변경:

- Lombok accessor를 제거한다.
- public constructor를 `private`로 변경한다.
- `enterBook(...)`와 `reconstitute(...)`만 생성 경로로 유지한다.
- explicit accessor를 추가한다.

권장 accessor:

```java
public Long no() { return no; }
public String title() { return title; }
public BookDesc desc() { return desc; }
public Classification classification() { return classification; }
public BookStatus bookStatus() { return bookStatus; }
public Location location() { return location; }
```

MapStruct가 private constructor를 직접 호출하지 못하면 `toDomain(...)`을 default method로 바꾸고 `Book.reconstitute(...)`를 호출한다. `toJpaEntity(...)` 매핑은 explicit accessor 이름에 맞춰 조정한다.

### 3. BestBook

대상:

- `bestbook-service/.../domain/model/BestBook.java`
- `BestBookPersistenceMapper`
- `BestBookResult`
- related tests

변경:

- Lombok accessor를 제거한다.
- public constructor를 `private`로 변경한다.
- 신규 생성은 `registerBestBook(...)`로 유지한다.
- 저장소 복원용 `reconstitute(...)`를 추가한다.
- explicit accessor를 추가한다.

권장 accessor:

```java
public Long id() { return id; }
public Long itemNo() { return itemNo; }
public String itemTitle() { return itemTitle; }
public long rentCount() { return rentCount; }
```

MapStruct가 private constructor를 직접 호출하지 못하면 `toDomain(...)`을 default method로 바꾸고 `BestBook.reconstitute(...)`를 호출한다. `resolveDocumentId(...)`는 `id()`와 `itemNo()`를 사용한다.

### 4. RentalSagaState

대상:

- `rental-service/.../domain/model/saga/RentalSagaState.java`

현재 기준에 대체로 맞는다. 다음만 확인한다.

- private constructor 유지
- `startRent`, `startReturn`, `startOverdue`, `reconstitute` 생성 경로 유지
- explicit accessor 유지
- Spring/JPA/Kafka/common-events/Lombok 의존 없음

## Domain Event 점검

aggregate-collected domain event가 있는 aggregate는 추가로 확인한다.

- `domain/event` 아래 top-level Java record인지 확인한다.
- marker interface를 구현하는지 확인한다.
- `common-events`, Kafka, Spring messaging, Avro generated class를 import하지 않는지 확인한다.
- `occurredAt`은 domain event 발생 시각으로만 사용한다.
- adapter가 eventId, correlationId, participant, saga step, topic을 부여하는지 확인한다.

## 검증

변경한 모듈 단위로 먼저 실행한다.

```powershell
.\gradlew.bat :member-service:test --tests com.example.library.member.architecture.HexagonalArchitectureTest
.\gradlew.bat :member-service:test

.\gradlew.bat :book-service:test --tests com.example.library.book.architecture.HexagonalArchitectureTest
.\gradlew.bat :book-service:test

.\gradlew.bat :bestbook-service:test --tests com.example.library.bestbook.architecture.HexagonalArchitectureTest
.\gradlew.bat :bestbook-service:test

.\gradlew.bat :rental-service:test --tests com.example.library.rental.architecture.HexagonalArchitectureTest
.\gradlew.bat :rental-service:test
```

MapStruct mapper 변경이 있으면 `compileJava`도 먼저 확인한다.

```powershell
.\gradlew.bat :book-service:compileJava
.\gradlew.bat :bestbook-service:compileJava
```
