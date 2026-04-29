package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

public class EventResult {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private EventType eventType;
    private boolean successed;
    private IDName idName;
    private Item item;
    private long point;
    private String reason;

    public EventResult() {
    }

    public EventResult(
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
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.successed = successed;
        this.idName = idName;
        this.item = item;
        this.point = point;
        this.reason = reason;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public boolean isSuccessed() {
        return successed;
    }

    public boolean getSuccessed() {
        return successed;
    }

    public void setSuccessed(boolean successed) {
        this.successed = successed;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
