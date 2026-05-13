package com.example.library.book.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookRentalEventServiceTest {
    @Mock
    private MakeAvailableBookUseCase makeAvailableBookUseCase;

    @Mock
    private MakeUnavailableBookUseCase makeUnavailableBookUseCase;

    @Mock
    private PublishBookRentalResultPort publishBookRentalResultPort;

    @Mock
    private MessageIdempotencyPort messageIdempotencyPort;

    @InjectMocks
    private BookRentalEventService service;

    @Test
    void rentFailureIsPublishedWhenUnavailableUseCaseFails() {
        ItemRented event = rentedEvent();
        given(messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RENTED
        )).willReturn(true);
        given(makeUnavailableBookUseCase.makeUnavailable(event.itemNo()))
            .willThrow(new IllegalArgumentException("book unavailable failed"));

        service.handleRent(event);

        EventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(event.eventId());
        assertThat(result.correlationId()).isEqualTo(event.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.RENT);
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_UNAVAILABLE);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("book unavailable failed");
    }

    @Test
    void returnFailureIsPublishedWhenAvailableUseCaseFails() {
        ItemReturned event = returnedEvent();
        given(messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )).willReturn(true);
        given(makeAvailableBookUseCase.makeAvailable(event.itemNo()))
            .willThrow(new IllegalArgumentException("book available failed"));

        service.handleReturn(event);

        EventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(event.eventId());
        assertThat(result.correlationId()).isEqualTo(event.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.RETURN);
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_AVAILABLE);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("book available failed");
    }

    private EventResult publishedResult() {
        ArgumentCaptor<EventResult> captor = ArgumentCaptor.forClass(EventResult.class);
        verify(publishBookRentalResultPort).publish(captor.capture());
        return captor.getValue();
    }

    private ItemRented rentedEvent() {
        return new ItemRented(
            "event-1",
            "correlation-1",
            Instant.parse("2026-05-13T00:00:00Z"),
            "member-1",
            "회원1",
            1L,
            "도서1",
            10L
        );
    }

    private ItemReturned returnedEvent() {
        return new ItemReturned(
            "event-2",
            "correlation-2",
            Instant.parse("2026-05-13T00:00:00Z"),
            "member-1",
            "회원1",
            1L,
            "도서1",
            10L
        );
    }
}
