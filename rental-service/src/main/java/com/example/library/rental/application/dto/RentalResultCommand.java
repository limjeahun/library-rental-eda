package com.example.library.rental.application.dto;

import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;

/**
 * 참여 서비스 결과 이벤트를 처리하기 위한 rental-service application command입니다.
 */
public record RentalResultCommand(
    String eventId,
    String correlationId,
    String sourceEventId,
    EventType eventType,
    Participant participant,
    SagaStep step,
    boolean successed,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point,
    String reason
) {
}
