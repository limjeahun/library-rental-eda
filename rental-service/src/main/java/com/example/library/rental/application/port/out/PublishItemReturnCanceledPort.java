package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;

/**
 * 반납 보상 완료 이벤트를 발행하는 outbound port입니다.
 */
public interface PublishItemReturnCanceledPort {
    void publishReturnCanceledEvent(ItemReturnCanceledDomainEvent event, String correlationId);
}
