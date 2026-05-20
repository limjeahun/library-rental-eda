package com.example.library.rental.domain.model.saga;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * rental-service 가 시작한 Kafka 기반 업무 흐름의 참여자 결과를 correlationId 기준으로 추적합니다.
 */
public class RentalSagaState {
    private final String correlationId;
    private String sourceEventId;
    private final RentalSagaType sagaType;
    private final RentalMember member;
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
        RentalSagaType sagaType,
        RentalMember member,
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
        this.sagaType = sagaType;
        this.member = member;
        this.item = item;
        this.point = point;
        this.bookResult = bookResult;
        this.memberResult = memberResult;
        this.sagaStatus = sagaStatus;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
    }

    public static RentalSagaState startRent(String correlationId, RentalMember member, RentalItem item, long point) {
        return start(correlationId, RentalSagaType.RENT, member, item, point, SagaParticipantStatus.PENDING);
    }

    public static RentalSagaState startReturn(String correlationId, RentalMember member, RentalItem item, long point) {
        return start(correlationId, RentalSagaType.RETURN, member, item, point, SagaParticipantStatus.PENDING);
    }

    public static RentalSagaState startOverdue(String correlationId, RentalMember member, long point) {
        return start(correlationId, RentalSagaType.OVERDUE, member, null, point, SagaParticipantStatus.NOT_REQUIRED);
    }

    public static RentalSagaState reconstitute(
        String correlationId,
        String sourceEventId,
        RentalSagaType sagaType,
        RentalMember member,
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
            sagaType,
            member,
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
        RentalSagaType sagaType,
        RentalMember member,
        RentalItem item,
        long point,
        SagaParticipantStatus bookResult
    ) {
        Instant now = Instant.now();
        return new RentalSagaState(
            correlationId,
            null,
            sagaType,
            member,
            item,
            point,
            bookResult,
            SagaParticipantStatus.PENDING,
            RentalSagaStatus.STARTED,
            now,
            now
        );
    }

    public void recordParticipantResult(
        String sourceEventId,
        RentalSagaParticipant participant,
        boolean successed
    ) {
        // 여러 참여자 결과가 와도 최초 원본 이벤트 ID만 SAGA 추적 상태에 남깁니다.
        if (this.sourceEventId == null || this.sourceEventId.isBlank()) {
            this.sourceEventId = sourceEventId;
        }
        if (participant == RentalSagaParticipant.BOOK && bookResult == SagaParticipantStatus.PENDING) {
            bookResult = toParticipantStatus(successed);
        } else if (participant == RentalSagaParticipant.MEMBER && memberResult == SagaParticipantStatus.PENDING) {
            memberResult = toParticipantStatus(successed);
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

    private SagaParticipantStatus toParticipantStatus(boolean successed) {
        return successed ? SagaParticipantStatus.SUCCESS : SagaParticipantStatus.FAILED;
    }

    public String correlationId() {
        return correlationId;
    }

    public String sourceEventId() {
        return sourceEventId;
    }

    public RentalSagaType sagaType() {
        return sagaType;
    }

    public RentalMember member() {
        return member;
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
