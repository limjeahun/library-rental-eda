package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import java.time.Instant;

/**
 * 대여 정지 해제를 위해 연체 포인트가 정산되었음을 알리는 도메인 이벤트입니다.
 */
public record OverdueCleared(
    String eventId,
    String correlationId,
    Instant occurredAt,
    IDName idName,
    long point
) {
}
