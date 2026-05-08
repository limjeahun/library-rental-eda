package com.example.library.rental.domain.model.saga;

/**
 * rental-service가 시작한 비동기 업무 흐름의 로컬 추적 상태입니다.
 */
public enum RentalSagaStatus {
    STARTED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
