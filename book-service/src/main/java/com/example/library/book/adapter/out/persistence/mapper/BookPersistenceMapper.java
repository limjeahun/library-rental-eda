package com.example.library.book.adapter.out.persistence.mapper;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.vo.BookDesc;
import org.springframework.stereotype.Component;

/**
 * 도서 도메인 모델과 JPA 엔티티 사이의 변환을 담당합니다.
 */
@Component
public class BookPersistenceMapper {
    /**
     * 도메인 도서를 JPA 저장용 엔티티로 변환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환합니다.
     */
    public BookJpaEntity toJpaEntity(Book book) {
        BookDesc desc = book.desc();
        return new BookJpaEntity(
            book.no(),
            book.title(),
            desc.description(),
            desc.author(),
            desc.isbn(),
            desc.publicationDate(),
            desc.source(),
            book.classification(),
            book.bookStatus(),
            book.location()
        );
    }

    /**
     * JPA 엔티티를 도메인 도서 모델로 복원합니다.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티입니다.
     * @return JPA 엔티티에서 복원한 도서 도메인 모델을 반환합니다.
     */
    public Book toDomain(BookJpaEntity entity) {
        return Book.reconstitute(
            entity.getNo(),
            entity.getTitle(),
            toBookDesc(entity),
            entity.getClassification(),
            entity.getBookStatus(),
            entity.getLocation()
        );
    }

    /**
     * JPA 엔티티의 flat 도서 상세 필드를 도메인 상세 값 객체로 변환합니다.
     *
     * @param entity 도서 상세 필드를 담은 저장소 엔티티입니다.
     * @return 도서 상세 값 객체를 반환합니다.
     */
    private BookDesc toBookDesc(BookJpaEntity entity) {
        return new BookDesc(
            entity.getDescription(),
            entity.getAuthor(),
            entity.getIsbn(),
            entity.getPublicationDate(),
            entity.getSource()
        );
    }
}
