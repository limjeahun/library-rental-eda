package com.example.library.rental.adapter.out.persistence.entity;

import com.example.library.common.event.EventType;
import com.example.library.rental.application.dto.RentalSagaStatus;
import com.example.library.rental.application.dto.SagaParticipantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * rental-service가 시작한 비동기 업무 흐름의 참여자 결과 상태를 저장하는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "rental_saga_states")
public class RentalSagaStateJpaEntity {
    @Id
    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "source_event_id", length = 120)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "item_no")
    private Long itemNo;

    @Column(name = "item_title")
    private String itemTitle;

    @Column(nullable = false)
    private long point;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_result", nullable = false, length = 40)
    private SagaParticipantStatus bookResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_result", nullable = false, length = 40)
    private SagaParticipantStatus memberResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false, length = 40)
    private RentalSagaStatus sagaStatus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RentalSagaStateJpaEntity() {
    }

    public RentalSagaStateJpaEntity(
        String correlationId,
        String sourceEventId,
        EventType eventType,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point,
        SagaParticipantStatus bookResult,
        SagaParticipantStatus memberResult,
        RentalSagaStatus sagaStatus,
        Instant startedAt,
        Instant updatedAt
    ) {
        this.correlationId = correlationId;
        this.sourceEventId = sourceEventId;
        this.eventType = eventType;
        this.memberId = memberId;
        this.memberName = memberName;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.point = point;
        this.bookResult = bookResult;
        this.memberResult = memberResult;
        this.sagaStatus = sagaStatus;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public Long getItemNo() {
        return itemNo;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public long getPoint() {
        return point;
    }

    public SagaParticipantStatus getBookResult() {
        return bookResult;
    }

    public SagaParticipantStatus getMemberResult() {
        return memberResult;
    }

    public RentalSagaStatus getSagaStatus() {
        return sagaStatus;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
