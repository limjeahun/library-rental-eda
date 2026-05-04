package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 대여 서비스가 도서 반납 완료 사실을 알리기 위해 발행하는 도메인 이벤트입니다.
 */
public record ItemReturned(
    String eventId,
    String correlationId,
    Instant occurredAt,
    IDName idName,
    Item item,
    long point
) {
    public ItemReturned(ItemRented itemRented) {
        this(
            itemRented.eventId(),
            itemRented.correlationId(),
            itemRented.occurredAt(),
            itemRented.idName(),
            itemRented.item(),
            itemRented.point()
        );
    }
}
