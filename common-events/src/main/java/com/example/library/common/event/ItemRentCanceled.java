package com.example.library.common.event;

import java.time.Instant;

/**
 * 대여 보상이 완료되어 이전 대여 반영을 되돌려야 함을 알리는 통합 이벤트입니다.
 */
public record ItemRentCanceled(
    String eventId,
    String correlationId,
    Instant occurredAt,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point
) {
}
