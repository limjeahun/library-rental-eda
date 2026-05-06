package com.example.library.common.event;

import java.time.Instant;

/**
 * 연체 해제 보상이 완료되어 이전 연체 해제 반영을 되돌려야 함을 알리는 통합 이벤트입니다.
 */
public record OverdueClearCanceled(
    String eventId,
    String correlationId,
    Instant occurredAt,
    String memberId,
    String memberName,
    long point
) {
}
