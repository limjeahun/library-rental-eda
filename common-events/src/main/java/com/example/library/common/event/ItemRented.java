package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

public class ItemRented {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private IDName idName;
    private Item item;
    private long point;

    public ItemRented() {
    }

    public ItemRented(String eventId, String correlationId, Instant occurredAt, IDName idName, Item item, long point) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.idName = idName;
        this.item = item;
        this.point = point;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public IDName getIdName() {
        return idName;
    }

    public void setIdName(IDName idName) {
        this.idName = idName;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
