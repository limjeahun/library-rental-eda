package com.example.library.book.adapter.out.persistence.mapper;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.vo.BookDesc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 도서 도메인 모델과 JPA 엔티티 사이의 변환을 담당합니다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookPersistenceMapper {
    /**
     * 도메인 도서를 JPA 저장용 엔티티로 변환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환합니다.
     */
    @Mapping(target = "description", source = "desc.description")
    @Mapping(target = "author", source = "desc.author")
    @Mapping(target = "isbn", source = "desc.isbn")
    @Mapping(target = "publicationDate", source = "desc.publicationDate")
    @Mapping(target = "source", source = "desc.source")
    BookJpaEntity toJpaEntity(Book book);

    /**
     * JPA 엔티티를 도메인 도서 모델로 복원합니다.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티입니다.
     * @return JPA 엔티티에서 복원한 도서 도메인 모델을 반환합니다.
     */
    @Mapping(target = "desc", source = ".")
    Book toDomain(BookJpaEntity entity);

    /**
     * JPA 엔티티의 flat 도서 상세 필드를 도메인 상세 값 객체로 변환합니다.
     *
     * @param entity 도서 상세 필드를 담은 저장소 엔티티입니다.
     * @return 도서 상세 값 객체를 반환합니다.
     */
    @Mapping(target = "description", source = "description")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "isbn", source = "isbn")
    @Mapping(target = "publicationDate", source = "publicationDate")
    @Mapping(target = "source", source = "source")
    BookDesc toBookDesc(BookJpaEntity entity);
}
