package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import java.time.Instant;

public class OverdueCleared {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private IDName idName;
    private long point;

    public OverdueCleared() {
    }

    public OverdueCleared(String eventId, String correlationId, Instant occurredAt, IDName idName, long point) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.idName = idName;
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

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
