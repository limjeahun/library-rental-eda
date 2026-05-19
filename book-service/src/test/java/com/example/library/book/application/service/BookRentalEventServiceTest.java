package com.example.library.book.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.book.application.dto.BookRentalEventCommand;
import com.example.library.book.application.port.out.LoadBookPort;
import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.application.port.out.SaveBookPort;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classification;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import com.example.library.book.domain.vo.BookDesc;
import com.example.library.common.event.InboundMessageType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookRentalEventServiceTest {
    @Mock
    private LoadBookPort loadBookPort;

    @Mock
    private SaveBookPort saveBookPort;

    @Mock
    private PublishBookRentalResultPort publishBookRentalResultPort;

    @Mock
    private MessageIdempotencyPort messageIdempotencyPort;

    @InjectMocks
    private BookRentalEventService service;

    @Test
    void rentSuccessPublishesAggregateDomainEvent() {
        BookRentalEventCommand command = rentedCommand();
        Book book = book(BookStatus.AVAILABLE);
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENTED
        )).willReturn(true);
        given(loadBookPort.loadBook(command.itemNo())).willReturn(book);

        service.handleRent(command);

        ArgumentCaptor<BookMadeUnavailableDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(BookMadeUnavailableDomainEvent.class);
        verify(saveBookPort).save(book);
        verify(publishBookRentalResultPort).publishBookMadeUnavailable(
            eventCaptor.capture(),
            eq(command.eventId()),
            eq(command.correlationId()),
            eq(command.memberId()),
            eq(command.memberName()),
            eq(command.point())
        );
        BookMadeUnavailableDomainEvent event = eventCaptor.getValue();
        assertThat(event.bookNo()).isEqualTo(command.itemNo());
        assertThat(event.title()).isEqualTo(command.itemTitle());
        assertThat(book.bookStatus()).isEqualTo(BookStatus.UNAVAILABLE);
        assertThat(book.pullDomainEvents()).isEmpty();
    }

    @Test
    void returnSuccessPublishesAggregateDomainEvent() {
        BookRentalEventCommand command = returnedCommand();
        Book book = book(BookStatus.UNAVAILABLE);
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )).willReturn(true);
        given(loadBookPort.loadBook(command.itemNo())).willReturn(book);

        service.handleReturn(command);

        ArgumentCaptor<BookMadeAvailableDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(BookMadeAvailableDomainEvent.class);
        verify(saveBookPort).save(book);
        verify(publishBookRentalResultPort).publishBookMadeAvailable(
            eventCaptor.capture(),
            eq(command.eventId()),
            eq(command.correlationId()),
            eq(command.memberId()),
            eq(command.memberName()),
            eq(command.point())
        );
        BookMadeAvailableDomainEvent event = eventCaptor.getValue();
        assertThat(event.bookNo()).isEqualTo(command.itemNo());
        assertThat(event.title()).isEqualTo(command.itemTitle());
        assertThat(book.bookStatus()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(book.pullDomainEvents()).isEmpty();
    }

    @Test
    void rentFailureIsPublishedWhenLoadBookFails() {
        BookRentalEventCommand command = rentedCommand();
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENTED
        )).willReturn(true);
        given(loadBookPort.loadBook(command.itemNo()))
            .willThrow(new IllegalArgumentException("book unavailable failed"));

        service.handleRent(command);

        verify(publishBookRentalResultPort).publishBookMakeUnavailableFailed(command, "book unavailable failed");
    }

    @Test
    void returnFailureIsPublishedWhenLoadBookFails() {
        BookRentalEventCommand command = returnedCommand();
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )).willReturn(true);
        given(loadBookPort.loadBook(command.itemNo()))
            .willThrow(new IllegalArgumentException("book available failed"));

        service.handleReturn(command);

        verify(publishBookRentalResultPort).publishBookMakeAvailableFailed(command, "book available failed");
    }

    private BookRentalEventCommand rentedCommand() {
        return new BookRentalEventCommand(
            "event-1",
            "correlation-1",
            "member-1",
            "회원1",
            1L,
            "도서1",
            10L
        );
    }

    private BookRentalEventCommand returnedCommand() {
        return new BookRentalEventCommand(
            "event-2",
            "correlation-2",
            "member-1",
            "회원1",
            1L,
            "도서1",
            10L
        );
    }

    private Book book(BookStatus status) {
        return Book.reconstitute(
            1L,
            "도서1",
            new BookDesc("설명", "저자", "isbn", LocalDate.now(), Source.SUPPLY),
            Classification.LITERATURE,
            status,
            Location.JEONGJA
        );
    }
}
