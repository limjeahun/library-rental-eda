package com.example.library.common.event;

import java.time.Instant;

/**
 * 대여 서비스가 도서 반납 완료 사실을 알리기 위해 발행하는 도메인 이벤트입니다.
 */
public record ItemReturned(
    String eventId,
    String correlationId,
    Instant occurredAt,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point
) {
    public ItemReturned(ItemRented itemRented) {
        this(
            itemRented.eventId(),
            itemRented.correlationId(),
            itemRented.occurredAt(),
            itemRented.memberId(),
            itemRented.memberName(),
            itemRented.itemNo(),
            itemRented.itemTitle(),
            itemRented.point()
        );
    }
}
