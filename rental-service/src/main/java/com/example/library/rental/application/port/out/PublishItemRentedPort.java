package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCardEvents;

/**
 * 도서 대여 완료 이벤트를 발행.
 */
public interface PublishItemRentedPort {
    /**
     * 도서 대여 완료 이벤트를 발행.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지.
     * @param correlationId 하나의 비동기 업무 흐름을 묶는 상관관계 식별자입니다.
     */
    void publishRentalEvent(RentalCardEvents.ItemRentedDomainEvent event, String correlationId);
}
