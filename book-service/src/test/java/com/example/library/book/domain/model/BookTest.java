package com.example.library.book.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.book.domain.event.BookDomainEvent;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.book.domain.vo.BookDesc;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void enterBook() {
        Book book = Book.enterBook("도서", desc(), Classification.LITERATURE, Location.JEONGJA);

        assertThat(book.title()).isEqualTo("도서");
        assertThat(book.bookStatus()).isEqualTo(BookStatus.ENTERED);
    }

    @Test
    void makeAvailable() {
        Book book = book(BookStatus.UNAVAILABLE);

        book.makeAvailable();

        assertThat(book.bookStatus()).isEqualTo(BookStatus.AVAILABLE);
        BookMadeAvailableDomainEvent event = pullSingleEvent(book, BookMadeAvailableDomainEvent.class);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.bookNo()).isEqualTo(1L);
        assertThat(event.title()).isEqualTo("도서");
        assertThat(book.pullDomainEvents()).isEmpty();
    }

    @Test
    void makeUnAvailable() {
        Book book = book(BookStatus.AVAILABLE);

        book.makeUnAvailable();

        assertThat(book.bookStatus()).isEqualTo(BookStatus.UNAVAILABLE);
        BookMadeUnavailableDomainEvent event = pullSingleEvent(book, BookMadeUnavailableDomainEvent.class);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.bookNo()).isEqualTo(1L);
        assertThat(event.title()).isEqualTo("도서");
        assertThat(book.pullDomainEvents()).isEmpty();
    }

    @Test
    void reconstitutedBookStartsWithoutDomainEvents() {
        Book book = book(BookStatus.AVAILABLE);

        assertThat(book.pullDomainEvents()).isEmpty();
    }

    private <T extends BookDomainEvent> T pullSingleEvent(Book book, Class<T> eventType) {
        List<BookDomainEvent> events = book.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(eventType);
        return eventType.cast(events.get(0));
    }

    private Book book(BookStatus status) {
        return Book.reconstitute(1L, "도서", desc(), Classification.LITERATURE, status, Location.JEONGJA);
    }

    private BookDesc desc() {
        return new BookDesc("설명", "저자", "isbn", LocalDate.now(), Source.SUPPLY);
    }
}
