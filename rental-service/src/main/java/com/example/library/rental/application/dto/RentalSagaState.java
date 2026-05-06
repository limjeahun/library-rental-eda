package com.example.library.rental.application.dto;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;
import java.time.Instant;

/**
 * rental-service가 시작한 Kafka 기반 업무 흐름의 참여자 결과를 correlationId 기준으로 추적합니다.
 */
public class RentalSagaState {
    private final String correlationId;
    private String sourceEventId;
    private final EventType eventType;
    private final RentalMember idName;
    private final RentalItem item;
    private final long point;
    private SagaParticipantStatus bookResult;
    private SagaParticipantStatus memberResult;
    private RentalSagaStatus sagaStatus;
    private final Instant startedAt;
    private Instant updatedAt;

    private RentalSagaState(
        String correlationId,
        String sourceEventId,
        EventType eventType,
        RentalMember idName,
        RentalItem item,
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
        this.idName = idName;
        this.item = item;
        this.point = point;
        this.bookResult = bookResult;
        this.memberResult = memberResult;
        this.sagaStatus = sagaStatus;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
    }

    public static RentalSagaState startRent(String correlationId, RentalMember idName, RentalItem item, long point) {
        return start(correlationId, EventType.RENT, idName, item, point, SagaParticipantStatus.PENDING);
    }

    public static RentalSagaState startReturn(String correlationId, RentalMember idName, RentalItem item, long point) {
        return start(correlationId, EventType.RETURN, idName, item, point, SagaParticipantStatus.PENDING);
    }

    public static RentalSagaState startOverdue(String correlationId, RentalMember idName, long point) {
        return start(correlationId, EventType.OVERDUE, idName, null, point, SagaParticipantStatus.NOT_REQUIRED);
    }

    public static RentalSagaState reconstitute(
        String correlationId,
        String sourceEventId,
        EventType eventType,
        RentalMember idName,
        RentalItem item,
        long point,
        SagaParticipantStatus bookResult,
        SagaParticipantStatus memberResult,
        RentalSagaStatus sagaStatus,
        Instant startedAt,
        Instant updatedAt
    ) {
        return new RentalSagaState(
            correlationId,
            sourceEventId,
            eventType,
            idName,
            item,
            point,
            bookResult,
            memberResult,
            sagaStatus,
            startedAt,
            updatedAt
        );
    }

    private static RentalSagaState start(
        String correlationId,
        EventType eventType,
        RentalMember idName,
        RentalItem item,
        long point,
        SagaParticipantStatus bookResult
    ) {
        Instant now = Instant.now();
        return new RentalSagaState(
            correlationId,
            null,
            eventType,
            idName,
            item,
            point,
            bookResult,
            SagaParticipantStatus.PENDING,
            RentalSagaStatus.STARTED,
            now,
            now
        );
    }

    public void recordResult(EventResult result) {
        if (sourceEventId == null || sourceEventId.isBlank()) {
            sourceEventId = result.sourceEventId();
        }
        if (result.participant() == Participant.BOOK && bookResult == SagaParticipantStatus.PENDING) {
            bookResult = toParticipantStatus(result);
        } else if (result.participant() == Participant.MEMBER && memberResult == SagaParticipantStatus.PENDING) {
            memberResult = toParticipantStatus(result);
        }
        refreshStatus();
    }

    public boolean hasFailure() {
        return bookResult == SagaParticipantStatus.FAILED || memberResult == SagaParticipantStatus.FAILED;
    }

    public boolean hasPendingParticipant() {
        return bookResult == SagaParticipantStatus.PENDING || memberResult == SagaParticipantStatus.PENDING;
    }

    public boolean isBookSuccess() {
        return bookResult == SagaParticipantStatus.SUCCESS;
    }

    public boolean isMemberSuccess() {
        return memberResult == SagaParticipantStatus.SUCCESS;
    }

    private void refreshStatus() {
        if (hasFailure()) {
            sagaStatus = hasPendingParticipant() ? RentalSagaStatus.COMPENSATING : RentalSagaStatus.COMPENSATED;
        } else if (!hasPendingParticipant()) {
            sagaStatus = RentalSagaStatus.COMPLETED;
        } else {
            sagaStatus = RentalSagaStatus.STARTED;
        }
        updatedAt = Instant.now();
    }

    private SagaParticipantStatus toParticipantStatus(EventResult result) {
        return result.successed() ? SagaParticipantStatus.SUCCESS : SagaParticipantStatus.FAILED;
    }

    public String correlationId() {
        return correlationId;
    }

    public String sourceEventId() {
        return sourceEventId;
    }

    public EventType eventType() {
        return eventType;
    }

    public RentalMember idName() {
        return idName;
    }

    public RentalItem item() {
        return item;
    }

    public long point() {
        return point;
    }

    public SagaParticipantStatus bookResult() {
        return bookResult;
    }

    public SagaParticipantStatus memberResult() {
        return memberResult;
    }

    public RentalSagaStatus sagaStatus() {
        return sagaStatus;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
