package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCardEvents;

/**
 * 연체 해제 보상 완료 이벤트를 발행하는 outbound port입니다.
 */
public interface PublishOverdueClearCanceledPort {
    void publishOverdueClearCanceledEvent(RentalCardEvents.OverdueClearCanceledDomainEvent event, String correlationId);
}
