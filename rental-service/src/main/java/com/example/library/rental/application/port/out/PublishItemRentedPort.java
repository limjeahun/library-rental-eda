package com.example.library.rental.application.port.out;

import com.example.library.common.event.ItemRented;

/**
 * 도서 대여 완료 이벤트를 발행.
 */
public interface PublishItemRentedPort {
    /**
     * 도서 대여 완료 이벤트를 발행.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지.
     */
    void publishRentalEvent(ItemRented event);
}
