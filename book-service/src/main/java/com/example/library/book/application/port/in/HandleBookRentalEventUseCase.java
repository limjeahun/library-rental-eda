package com.example.library.book.application.port.in;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;

/**
 * 대여 이벤트는 도서를 UNAVAILABLE로, 반납 이벤트는 AVAILABLE로 바꾸고 성공/실패 결과 이벤트를 만드는 application 계약입니다.
 */
public interface HandleBookRentalEventUseCase {
    /**
     * 도서 대여 완료 이벤트를 처리합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void handleRent(ItemRented event);

    /**
     * 도서 반납 완료 이벤트를 처리합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void handleReturn(ItemReturned event);
}
