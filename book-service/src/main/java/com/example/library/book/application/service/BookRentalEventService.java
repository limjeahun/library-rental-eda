package com.example.library.book.application.service;

import com.example.library.book.application.dto.BookRentalCancelCommand;
import com.example.library.book.application.dto.BookRentalEventCommand;
import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.book.application.port.out.LoadBookPort;
import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.application.port.out.SaveBookPort;
import com.example.library.book.domain.event.BookDomainEvent;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.book.domain.model.Book;
import com.example.library.common.event.InboundMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 이벤트를 받아 도서 상태를 변경하고 처리 결과 이벤트를 발행하는 application service.
 *
 * <p>상태 변경 전에 processed message를 먼저 기록해 Kafka 재전달로 인한 중복 도서 상태 변경을 막습니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookRentalEventService implements HandleBookRentalEventUseCase {
    private final PublishBookRentalResultPort publishBookRentalResultPort;
    private final MessageIdempotencyPort messageIdempotencyPort;
    private final LoadBookPort loadBookPort;
    private final SaveBookPort saveBookPort;

    /**
     * 도서 대여 이벤트를 처리해 도서를 대여 불가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param command 처리할 대여 이벤트 application command입니다.
     */
    @Override
    @Transactional
    public void handleRent(BookRentalEventCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENTED
        )) {
            log.info("skip already processed book rent eventId={}", command.eventId());
            return;
        }
        try {
            Book book = makeUnavailable(command.itemNo());
            var event = pullRequiredEvent(book, BookMadeUnavailableDomainEvent.class);
            publishBookRentalResultPort.publishBookMadeUnavailable(
                event,
                command.eventId(),
                command.correlationId(),
                command.memberId(),
                command.memberName(),
                command.point()
            );
        } catch (Exception ex) {
            log.error("Book rent event failed eventId={}", command.eventId(), ex);
            publishBookRentalResultPort.publishBookMakeUnavailableFailed(command, ex.getMessage());
        }
    }

    /**
     * 도서 반납 이벤트를 처리해 도서를 대여 가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param command 처리할 반납 이벤트 application command입니다.
     */
    @Override
    @Transactional
    public void handleReturn(BookRentalEventCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )) {
            log.info("skip already processed book return eventId={}", command.eventId());
            return;
        }
        try {
            Book book = makeAvailable(command.itemNo());
            var event = pullRequiredEvent(book, BookMadeAvailableDomainEvent.class);
            publishBookRentalResultPort.publishBookMadeAvailable(
                event,
                command.eventId(),
                command.correlationId(),
                command.memberId(),
                command.memberName(),
                command.point()
            );
        } catch (Exception ex) {
            log.error("Book return event failed eventId={}", command.eventId(), ex);
            publishBookRentalResultPort.publishBookMakeAvailableFailed(command, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleRentCanceled(BookRentalCancelCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENT_CANCELED
        )) {
            log.info("skip already processed book rent cancel eventId={}", command.eventId());
            return;
        }
        makeAvailable(command.itemNo());
    }

    @Override
    @Transactional
    public void handleReturnCanceled(BookRentalCancelCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RETURN_CANCELED
        )) {
            log.info("skip already processed book return cancel eventId={}", command.eventId());
            return;
        }
        makeUnavailable(command.itemNo());
    }

    /**
     * 도서를 대여 가능한 상태로 변경
     *
     * @param bookNo 도서 번호.
     */
    private Book makeAvailable(long bookNo) {
        Book book = loadBookPort.loadBookForUpdate(bookNo);
        book.makeAvailable();
        saveBookPort.save(book);
        return book;
    }

    /**
     * 도서를 대여 불가능한 상태로 변경.
     *
     * @param bookNo 도서 번호.
     */
    private Book makeUnavailable(long bookNo) {
        Book book = loadBookPort.loadBookForUpdate(bookNo);
        book.makeUnAvailable();
        saveBookPort.save(book);
        return book;
    }

    private <T extends BookDomainEvent> T pullRequiredEvent(Book book, Class<T> eventType) {
        return book.pullDomainEvents()
            .stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Expected domain event was not raised: " + eventType.getSimpleName()
            ));
    }
}
