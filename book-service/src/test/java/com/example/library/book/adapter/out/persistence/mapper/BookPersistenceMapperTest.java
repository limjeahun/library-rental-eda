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

class BookPersistenceMapperTest {
    private final BookPersistenceMapper mapper = new BookPersistenceMapper();

    @Test
    void mapsBookToJpaEntity() {
        Book book = book();

        BookJpaEntity entity = mapper.toJpaEntity(book);

        assertThat(entity.getNo()).isEqualTo(book.no());
        assertThat(entity.getTitle()).isEqualTo(book.title());
        assertThat(entity.getDescription()).isEqualTo(book.desc().description());
        assertThat(entity.getAuthor()).isEqualTo(book.desc().author());
        assertThat(entity.getIsbn()).isEqualTo(book.desc().isbn());
        assertThat(entity.getPublicationDate()).isEqualTo(book.desc().publicationDate());
        assertThat(entity.getSource()).isEqualTo(book.desc().source());
        assertThat(entity.getClassification()).isEqualTo(book.classification());
        assertThat(entity.getBookStatus()).isEqualTo(book.bookStatus());
        assertThat(entity.getLocation()).isEqualTo(book.location());
    }

    @Test
    void mapsJpaEntityToBook() {
        BookJpaEntity entity = entity();

        Book book = mapper.toDomain(entity);

        assertThat(book.no()).isEqualTo(entity.getNo());
        assertThat(book.title()).isEqualTo(entity.getTitle());
        assertThat(book.desc().description()).isEqualTo(entity.getDescription());
        assertThat(book.desc().author()).isEqualTo(entity.getAuthor());
        assertThat(book.desc().isbn()).isEqualTo(entity.getIsbn());
        assertThat(book.desc().publicationDate()).isEqualTo(entity.getPublicationDate());
        assertThat(book.desc().source()).isEqualTo(entity.getSource());
        assertThat(book.classification()).isEqualTo(entity.getClassification());
        assertThat(book.bookStatus()).isEqualTo(entity.getBookStatus());
        assertThat(book.location()).isEqualTo(entity.getLocation());
    }

    private Book book() {
        return Book.reconstitute(
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
