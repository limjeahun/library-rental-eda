package com.example.library.rental.application.port.out;

import com.example.library.common.event.ItemReturned;

/**
 * 도서 반납 완료 이벤트를 발행하는 outbound port입니다.
 */
public interface PublishItemReturnedPort {
    /**
     * 도서 반납 완료 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void publishReturnEvent(ItemReturned event);
}
