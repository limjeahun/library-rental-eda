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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 대여 이벤트를 받아 도서 상태를 변경하고 처리 결과 이벤트를 발행하는 application service입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookRentalEventService implements HandleBookRentalEventUseCase {
    private final MakeAvailableBookUseCase makeAvailableBookUseCase;
    private final MakeUnavailableBookUseCase makeUnavailableBookUseCase;
    private final BookEventOutputPort bookEventOutputPort;
    @Value("${app.failure.force-rent-fail:false}")
    private final boolean forceRentFail;
    @Value("${app.failure.force-return-fail:false}")
    private final boolean forceReturnFail;

    /**
     * 도서 대여 이벤트를 처리해 도서를 대여 불가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
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

    /**
     * 도서 반납 이벤트를 처리해 도서를 대여 가능 상태로 바꾸고 결과 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
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

    /**
     * 대여 서비스가 보상 여부를 판단할 수 있도록 처리 결과 이벤트를 생성합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     * @param eventType 결과 이벤트가 대응하는 대여 흐름 타입입니다.
     * @param successed 참여 서비스 처리 성공 여부입니다.
     * @param reason 실패 결과나 보상 command의 사유입니다.
     * @return 도서 상태 변경 성공/실패, 회원, 도서, 포인트, 사유를 담은 EventResult를 반환합니다.
     */
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
