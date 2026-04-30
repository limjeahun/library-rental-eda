package com.example.library.book.application.service;

import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.book.application.port.out.BookEventOutputPort;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookRentalEventService implements HandleBookRentalEventUseCase {
    private static final Logger log = LoggerFactory.getLogger(BookRentalEventService.class);

    private final MakeAvailableBookUseCase makeAvailableBookUseCase;
    private final MakeUnavailableBookUseCase makeUnavailableBookUseCase;
    private final BookEventOutputPort bookEventOutputPort;
    private final boolean forceRentFail;
    private final boolean forceReturnFail;

    public BookRentalEventService(
        MakeAvailableBookUseCase makeAvailableBookUseCase,
        MakeUnavailableBookUseCase makeUnavailableBookUseCase,
        BookEventOutputPort bookEventOutputPort,
        @Value("${app.failure.force-rent-fail:false}") boolean forceRentFail,
        @Value("${app.failure.force-return-fail:false}") boolean forceReturnFail
    ) {
        this.makeAvailableBookUseCase = makeAvailableBookUseCase;
        this.makeUnavailableBookUseCase = makeUnavailableBookUseCase;
        this.bookEventOutputPort = bookEventOutputPort;
        this.forceRentFail = forceRentFail;
        this.forceReturnFail = forceReturnFail;
    }

    @Override
    public void handleRent(ItemRented event) {
        try {
            if (forceRentFail) {
                throw new IllegalArgumentException("forced rental_rent failure");
            }
            makeUnavailableBookUseCase.makeUnavailable(event.getItem().getNo());
            bookEventOutputPort.publish(result(event, EventType.RENT, true, null));
        } catch (Exception ex) {
            log.error("Book rent event failed eventId={}", event.getEventId(), ex);
            bookEventOutputPort.publish(result(event, EventType.RENT, false, ex.getMessage()));
        }
    }

    @Override
    public void handleReturn(ItemReturned event) {
        try {
            if (forceReturnFail) {
                throw new IllegalArgumentException("forced rental_return failure");
            }
            makeAvailableBookUseCase.makeAvailable(event.getItem().getNo());
            bookEventOutputPort.publish(result(event, EventType.RETURN, true, null));
        } catch (Exception ex) {
            log.error("Book return event failed eventId={}", event.getEventId(), ex);
            bookEventOutputPort.publish(result(event, EventType.RETURN, false, ex.getMessage()));
        }
    }

    private EventResult result(ItemRented event, EventType eventType, boolean successed, String reason) {
        return new EventResult(
            event.getEventId(),
            event.getCorrelationId(),
            Instant.now(),
            eventType,
            successed,
            event.getIdName(),
            event.getItem(),
            event.getPoint(),
            reason
        );
    }
}
