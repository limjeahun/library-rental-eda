package com.example.library.book.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void enterBook() {
        Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA);

        assertThat(book.getTitle()).isEqualTo("도서");
        assertThat(book.getBookStatus()).isEqualTo(BookStatus.ENTERED);
    }

    @Test
    void makeAvailable() {
        Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA);

        book.makeAvailable();

        assertThat(book.getBookStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void makeUnAvailable() {
        Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA);

        book.makeUnAvailable();

        assertThat(book.getBookStatus()).isEqualTo(BookStatus.UNAVAILABLE);
    }

    private BookDesc desc() {
        return new BookDesc("설명", "저자", "isbn", LocalDate.now(), Source.SUPPLY);
    }
}
