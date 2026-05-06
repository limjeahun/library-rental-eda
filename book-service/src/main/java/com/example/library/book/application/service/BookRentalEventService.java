package com.example.library.book.application.service;

import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.book.application.port.out.BookRentalFailurePolicyPort;
import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturnCanceled;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 이벤트를 받아 도서 상태를 변경하고 처리 결과 이벤트를 발행하는 application service입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookRentalEventService implements HandleBookRentalEventUseCase {
    private static final String SERVICE_NAME = "book-service";

    private final MakeAvailableBookUseCase makeAvailableBookUseCase;
    private final MakeUnavailableBookUseCase makeUnavailableBookUseCase;
    private final PublishBookRentalResultPort publishBookRentalResultPort;
    private final MessageIdempotencyPort messageIdempotencyPort;
    private final BookRentalFailurePolicyPort failurePolicyPort;

    /**
     * 도서 대여 이벤트를 처리해 도서를 대여 불가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    @Transactional
    public void handleRent(ItemRented event) {
        if (!messageIdempotencyPort.markProcessed(SERVICE_NAME, event.eventId(), event.correlationId(), "ItemRented")) {
            log.info("skip already processed book rent eventId={}", event.eventId());
            return;
        }
        try {
            if (failurePolicyPort.shouldFailRent()) {
                throw new IllegalArgumentException("forced rental_rent failure");
            }
            makeUnavailableBookUseCase.makeUnavailable(event.itemNo());
            publishBookRentalResultPort.publish(EventResult.success(
                event.eventId(),
                event.correlationId(),
                EventType.RENT,
                Participant.BOOK,
                SagaStep.BOOK_MAKE_UNAVAILABLE,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point()
            ));
        } catch (Exception ex) {
            log.error("Book rent event failed eventId={}", event.eventId(), ex);
            publishBookRentalResultPort.publish(EventResult.failure(
                event.eventId(),
                event.correlationId(),
                EventType.RENT,
                Participant.BOOK,
                SagaStep.BOOK_MAKE_UNAVAILABLE,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point(),
                ex.getMessage()
            ));
        }
    }

    /**
     * 도서 반납 이벤트를 처리해 도서를 대여 가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    @Transactional
    public void handleReturn(ItemReturned event) {
        if (!messageIdempotencyPort.markProcessed(SERVICE_NAME, event.eventId(), event.correlationId(), "ItemReturned")) {
            log.info("skip already processed book return eventId={}", event.eventId());
            return;
        }
        try {
            if (failurePolicyPort.shouldFailReturn()) {
                throw new IllegalArgumentException("forced rental_return failure");
            }
            makeAvailableBookUseCase.makeAvailable(event.itemNo());
            publishBookRentalResultPort.publish(EventResult.success(
                event.eventId(),
                event.correlationId(),
                EventType.RETURN,
                Participant.BOOK,
                SagaStep.BOOK_MAKE_AVAILABLE,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point()
            ));
        } catch (Exception ex) {
            log.error("Book return event failed eventId={}", event.eventId(), ex);
            publishBookRentalResultPort.publish(EventResult.failure(
                event.eventId(),
                event.correlationId(),
                EventType.RETURN,
                Participant.BOOK,
                SagaStep.BOOK_MAKE_AVAILABLE,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point(),
                ex.getMessage()
            ));
        }
    }

    @Override
    @Transactional
    public void handleRentCanceled(ItemRentCanceled event) {
        if (!messageIdempotencyPort.markProcessed(SERVICE_NAME, event.eventId(), event.correlationId(), "ItemRentCanceled")) {
            log.info("skip already processed book rent cancel eventId={}", event.eventId());
            return;
        }
        makeAvailableBookUseCase.makeAvailable(event.itemNo());
    }

    @Override
    @Transactional
    public void handleReturnCanceled(ItemReturnCanceled event) {
        if (!messageIdempotencyPort.markProcessed(SERVICE_NAME, event.eventId(), event.correlationId(), "ItemReturnCanceled")) {
            log.info("skip already processed book return cancel eventId={}", event.eventId());
            return;
        }
        makeUnavailableBookUseCase.makeUnavailable(event.itemNo());
    }
}
