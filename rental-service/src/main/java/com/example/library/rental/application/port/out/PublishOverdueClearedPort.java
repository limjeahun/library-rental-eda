package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCardEvents;

/**
 * 연체 해제 완료 이벤트를 발행하는 outbound port입니다.
 */
public interface PublishOverdueClearedPort {
    /**
     * 연체 해제 완료 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     * @param correlationId 하나의 비동기 업무 흐름을 묶는 상관관계 식별자입니다.
     */
    void publishOverdueClearEvent(RentalCardEvents.OverdueClearedDomainEvent event, String correlationId);
}
