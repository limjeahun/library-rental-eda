package com.example.library.book.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.book.application.dto.BookRentalEventCommand;
import com.example.library.book.application.dto.BookRentalEventResult;
import com.example.library.book.application.port.out.LoadBookPort;
import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.application.port.out.SaveBookPort;
import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
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

        BookRentalEventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(command.eventId());
        assertThat(result.correlationId()).isEqualTo(command.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.RENT);
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_UNAVAILABLE);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("book unavailable failed");
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

        BookRentalEventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(command.eventId());
        assertThat(result.correlationId()).isEqualTo(command.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.RETURN);
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_AVAILABLE);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("book available failed");
    }

    private BookRentalEventResult publishedResult() {
        ArgumentCaptor<BookRentalEventResult> captor = ArgumentCaptor.forClass(BookRentalEventResult.class);
        verify(publishBookRentalResultPort).publish(captor.capture());
        return captor.getValue();
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
}
