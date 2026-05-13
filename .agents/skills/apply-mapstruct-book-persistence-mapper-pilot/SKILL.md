---
name: apply-mapstruct-book-persistence-mapper-pilot
description: library-rental-eda에서 BookPersistenceMapper 하나만 MapStruct pilot으로 전환하거나 리뷰할 때 사용한다. Trigger when introducing MapStruct code generation only for book-service persistence mapping between BookJpaEntity and Book, evaluating MapStruct fit before broader mapper adoption, adding MapStruct Gradle annotation processor setup, or ensuring generated mapper use preserves DDD/Hexagonal boundaries. Do not use for AvroMessageMapper, web Request/Response DTO mapping, common-events contracts, or full-project mapper migration unless explicitly requested.
---

# MapStruct Book Persistence Mapper Pilot

## 목적

이 스킬은 `library-rental-eda`에서 `book-service`의 `BookPersistenceMapper`만 MapStruct 기반으로 전환하는 pilot 작업을 안전하게 수행하기 위한 절차다. 목표는 코드 생성 도구 적합성을 검증하는 것이며, 전체 Mapper 일괄 전환이 아니다.

## 필수 읽기

작업 전에 다음을 확인한다.

1. `AGENTS.md`를 최우선 규칙으로 따른다.
2. 계층 경계가 애매하면 `docs/architecture-rule-eda.md`의 DTO/Mapper/adapter 규칙을 참고한다.
3. 현재 대상 파일을 직접 읽는다.
   - `book-service/src/main/java/com/example/library/book/adapter/out/persistence/mapper/BookPersistenceMapper.java`
   - `book-service/src/main/java/com/example/library/book/adapter/out/persistence/entity/BookJpaEntity.java`
   - `book-service/src/main/java/com/example/library/book/domain/model/Book.java`
   - `book-service/src/main/java/com/example/library/book/domain/vo/BookDesc.java`
   - `build.gradle.kts`

## 범위

수정 범위는 기본적으로 다음으로 제한한다.

- `build.gradle.kts`의 MapStruct 의존성 및 annotation processor 설정
- `BookPersistenceMapper`의 MapStruct mapper 전환
- 필요한 경우 `book-service`의 mapper-focused 테스트

다음은 pilot 범위가 아니다.

- `common-events`의 `AvroMessageMapper`
- Web Request `toCommand()` 또는 Response `from(result)` 변환
- `member-service`, `rental-service`, `bestbook-service` mapper 전환
- domain model, application DTO, Kafka contract 구조 변경
- `common-core`에 공통 Mapper 유틸 추가

## 아키텍처 규칙

- MapStruct mapper는 adapter outbound 경계인 `book.adapter.out.persistence.mapper`에 둔다.
- domain/application 계층이 MapStruct annotation 또는 generated mapper를 알게 하지 않는다.
- mapper는 persistence representation과 domain representation 사이에서만 domain VO를 생성한다.
- `BookDesc` 생성, `Book` 생성자 호출, `BookJpaEntity` 생성자 호출처럼 도메인 복원 의도가 드러나야 한다.
- generated code를 커밋하지 않는다. `build/generated/sources/annotationProcessor/java/main`은 빌드 산출물이다.

## Gradle 적용 기준

pilot 단계에서는 MapStruct 의존성을 전체 `serviceProjects`가 아니라 `project(":book-service")`에만 추가한다.

권장 형태:

```kotlin
val mapstructVersion = "<검증한 안정 버전>"
val lombokMapstructBindingVersion = "0.2.0"

project(":book-service") {
    dependencies {
        "implementation"("org.mapstruct:mapstruct:$mapstructVersion")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:$mapstructVersion")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    }
}
```

MapStruct 버전은 작업 시점의 안정 버전을 확인해 선택한다. 이미 프로젝트 또는 조직 기준 버전이 있으면 그 값을 우선한다. `Book`은 Lombok getter를 사용하므로 Lombok과 MapStruct annotation processor 연동을 확인한다.

## 구현 지침

`BookPersistenceMapper`는 class에서 interface 또는 abstract class 기반 `@Mapper(componentModel = "spring")`로 전환한다.

단순 필드명 불일치와 nested VO 매핑을 명시한다.

- `book.desc.description()` ↔ `entity.description`
- `book.desc.author()` ↔ `entity.author`
- `book.desc.isbn()` ↔ `entity.isbn`
- `book.desc.publicationDate()` ↔ `entity.publicationDate`
- `book.desc.source()` ↔ `entity.source`

MapStruct가 생성하기 어려운 복원은 `default` method 또는 `@ObjectFactory`로 직접 작성한다. 자동 매핑을 억지로 늘리기보다 생성자 기반 복원 의도가 선명한 코드를 우선한다.

예상 방향:

```java
@Mapper(componentModel = "spring")
public interface BookPersistenceMapper {
    BookJpaEntity toJpaEntity(Book book);

    default Book toDomain(BookJpaEntity entity) {
        return new Book(
            entity.getNo(),
            entity.getTitle(),
            new BookDesc(
                entity.getDescription(),
                entity.getAuthor(),
                entity.getIsbn(),
                entity.getPublicationDate(),
                entity.getSource()
            ),
            entity.getClassification(),
            entity.getBookStatus(),
            entity.getLocation()
        );
    }
}
```

`toJpaEntity`도 MapStruct보다 수동 생성자 호출이 더 명확하면 `default` method로 남겨도 된다. 이 경우 pilot의 가치는 Gradle/DI/annotation processor 적합성 확인과 향후 mapper별 판단 기준 수립에 둔다.

## 검증

최소 검증:

```powershell
.\gradlew.bat :book-service:compileJava
.\gradlew.bat :book-service:test --tests com.example.library.book.architecture.HexagonalArchitectureTest
```

가능하면 mapper 동등성 테스트를 추가하거나 기존 테스트로 다음을 확인한다.

- `Book -> BookJpaEntity` 변환 값이 기존 수동 mapper와 동일하다.
- `BookJpaEntity -> Book` 복원 시 `BookDesc`와 enum 값이 유지된다.
- Spring bean 주입이 기존 `BookPersistenceAdapter`에서 그대로 동작한다.

## 판단 기준

pilot 후 다음 기준으로 확대 여부를 보고한다.

- 설정 복잡도가 수동 mapper 감소 효과보다 큰가?
- nested VO 변환에 `default` method가 너무 많이 필요하지 않은가?
- mapper 오류가 컴파일 타임에 잘 드러나는가?
- ArchUnit 경계가 유지되는가?
- generated code와 수동 코드 사이에서 디버깅 부담이 늘지 않는가?

결론 보고는 `BookPersistenceMapper` 기준으로만 한다. 다른 서비스로 확대하려면 별도 요청이나 후속 계획으로 분리한다.
