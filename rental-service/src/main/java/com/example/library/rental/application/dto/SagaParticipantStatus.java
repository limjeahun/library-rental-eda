package com.example.library.rental.application.dto;

/**
 * SAGA 참여자별 처리 결과 상태입니다.
 */
public enum SagaParticipantStatus {
    PENDING,
    SUCCESS,
    FAILED,
    NOT_REQUIRED
}
