package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 참여 서비스가 Kafka command/event 처리 결과를 대여 서비스로 회신하는 결과 이벤트입니다.
 */
public record EventResult(
    String eventId,
    String correlationId,
    Instant occurredAt,
    EventType eventType,
    boolean successed,
    IDName idName,
    Item item,
    long point,
    String reason
) {
}
