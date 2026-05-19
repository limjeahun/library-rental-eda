package com.example.library.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 참여 서비스가 Kafka command/event 처리 결과를 대여 서비스로 회신하는 결과 이벤트입니다.
 */
public record EventResult(
    String eventId,
    String correlationId,
    String sourceEventId,
    Instant occurredAt,
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
    public static EventResult success(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        Participant participant,
        SagaStep step,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point
    ) {
        return success(
            sourceEventId,
            correlationId,
            eventType,
            participant,
            step,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point,
            Instant.now()
        );
    }

    public static EventResult success(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        Participant participant,
        SagaStep step,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point,
        Instant occurredAt
    ) {
        return result(
            sourceEventId,
            correlationId,
            eventType,
            participant,
            step,
            true,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point,
            null,
            occurredAt
        );
    }

    public static EventResult failure(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        Participant participant,
        SagaStep step,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point,
        String reason
    ) {
        return result(
            sourceEventId,
            correlationId,
            eventType,
            participant,
            step,
            false,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point,
            reason,
            Instant.now()
        );
    }

    private static EventResult result(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        Participant participant,
        SagaStep step,
        boolean successed,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point,
        String reason,
        Instant occurredAt
    ) {
        String eventId = UUID.randomUUID().toString();
        validateSnapshot(eventType, memberId, itemNo, itemTitle);
        return new EventResult(
            eventId,
            normalizeCorrelationId(correlationId, eventId),
            sourceEventId,
            occurredAt,
            eventType,
            participant,
            step,
            successed,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point,
            reason
        );
    }

    private static String normalizeCorrelationId(String correlationId, String eventId) {
        return correlationId == null || correlationId.isBlank() ? eventId : correlationId;
    }

    private static void validateSnapshot(EventType eventType, String memberId, Long itemNo, String itemTitle) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("memberId는 비어 있을 수 없습니다.");
        }
        if ((eventType == EventType.RENT || eventType == EventType.RETURN) && itemNo == null) {
            throw new IllegalArgumentException("RENT/RETURN 결과 이벤트는 itemNo가 필요합니다.");
        }
        if ((eventType == EventType.RENT || eventType == EventType.RETURN)
            && (itemTitle == null || itemTitle.isBlank())) {
            throw new IllegalArgumentException("RENT/RETURN 결과 이벤트는 itemTitle이 필요합니다.");
        }
    }
}
