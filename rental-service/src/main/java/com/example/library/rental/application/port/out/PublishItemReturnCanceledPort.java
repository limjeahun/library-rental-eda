package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCardEvents;

/**
 * 반납 보상 완료 이벤트를 발행하는 outbound port입니다.
 */
public interface PublishItemReturnCanceledPort {
    void publishReturnCanceledEvent(RentalCardEvents.ItemReturnCanceledDomainEvent event, String correlationId);
}
