package com.example.library.rental.domain.model.saga;

/**
 * SAGA 참여자별 처리 결과 상태입니다.
 */
public enum SagaParticipantStatus {
    PENDING,
    SUCCESS,
    FAILED,
    NOT_REQUIRED
}
