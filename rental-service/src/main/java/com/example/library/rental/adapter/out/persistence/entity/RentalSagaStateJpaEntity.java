package com.example.library.rental.adapter.out.persistence.entity;

import com.example.library.rental.domain.model.saga.RentalSagaStatus;
import com.example.library.rental.domain.model.saga.RentalSagaType;
import com.example.library.rental.domain.model.saga.SagaParticipantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * rental-service 가 시작한 비동기 업무 흐름의 참여자 결과 상태를 저장하는 JPA 엔티티입니다.
 */
@Getter
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
    private RentalSagaType sagaType;

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
        RentalSagaType sagaType,
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
        this.sagaType = sagaType;
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

}
