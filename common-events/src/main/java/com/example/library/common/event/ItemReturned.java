package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

public class ItemReturned extends ItemRented {
    public ItemReturned() {
        super();
    }

    public ItemReturned(String eventId, String correlationId, Instant occurredAt, IDName idName, Item item, long point) {
        super(eventId, correlationId, occurredAt, idName, item, point);
    }

    public ItemReturned(ItemRented itemRented) {
        super(
            itemRented.getEventId(),
            itemRented.getCorrelationId(),
            itemRented.getOccurredAt(),
            itemRented.getIdName(),
            itemRented.getItem(),
            itemRented.getPoint()
        );
    }
}
