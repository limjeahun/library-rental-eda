package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 대여 서비스가 도서 대여 완료 사실을 알리기 위해 발행하는 도메인 이벤트.
 */
public record ItemRented(
    String eventId,
    String correlationId,
    Instant occurredAt,
    IDName idName,
    Item item,
    long point
) {
}
