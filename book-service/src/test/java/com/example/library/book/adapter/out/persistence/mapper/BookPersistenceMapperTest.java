package com.example.library.book.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classification;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import com.example.library.book.domain.vo.BookDesc;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BookPersistenceMapperTest {
    private final BookPersistenceMapper mapper = Mappers.getMapper(BookPersistenceMapper.class);

    @Test
    void mapsBookToJpaEntity() {
        Book book = book();

        BookJpaEntity entity = mapper.toJpaEntity(book);

        assertThat(entity.getNo()).isEqualTo(book.getNo());
        assertThat(entity.getTitle()).isEqualTo(book.getTitle());
        assertThat(entity.getDescription()).isEqualTo(book.getDesc().description());
        assertThat(entity.getAuthor()).isEqualTo(book.getDesc().author());
        assertThat(entity.getIsbn()).isEqualTo(book.getDesc().isbn());
        assertThat(entity.getPublicationDate()).isEqualTo(book.getDesc().publicationDate());
        assertThat(entity.getSource()).isEqualTo(book.getDesc().source());
        assertThat(entity.getClassification()).isEqualTo(book.getClassification());
        assertThat(entity.getBookStatus()).isEqualTo(book.getBookStatus());
        assertThat(entity.getLocation()).isEqualTo(book.getLocation());
    }

    @Test
    void mapsJpaEntityToBook() {
        BookJpaEntity entity = entity();

        Book book = mapper.toDomain(entity);

        assertThat(book.getNo()).isEqualTo(entity.getNo());
        assertThat(book.getTitle()).isEqualTo(entity.getTitle());
        assertThat(book.getDesc().description()).isEqualTo(entity.getDescription());
        assertThat(book.getDesc().author()).isEqualTo(entity.getAuthor());
        assertThat(book.getDesc().isbn()).isEqualTo(entity.getIsbn());
        assertThat(book.getDesc().publicationDate()).isEqualTo(entity.getPublicationDate());
        assertThat(book.getDesc().source()).isEqualTo(entity.getSource());
        assertThat(book.getClassification()).isEqualTo(entity.getClassification());
        assertThat(book.getBookStatus()).isEqualTo(entity.getBookStatus());
        assertThat(book.getLocation()).isEqualTo(entity.getLocation());
    }

    private Book book() {
        return new Book(
            1L,
            "도서1",
            new BookDesc(
                "설명",
                "작가",
                "9781234567890",
                LocalDate.of(2026, 5, 13),
                Source.DONATION
            ),
            Classification.LITERATURE,
            BookStatus.AVAILABLE,
            Location.PANGYO
        );
    }

    private BookJpaEntity entity() {
        return new BookJpaEntity(
            2L,
            "도서2",
            "설명2",
            "작가2",
            "9781234567891",
            LocalDate.of(2026, 5, 14),
            Source.SUPPLY,
            Classification.COMPUTER,
            BookStatus.UNAVAILABLE,
            Location.JEONGJA
        );
    }
}
