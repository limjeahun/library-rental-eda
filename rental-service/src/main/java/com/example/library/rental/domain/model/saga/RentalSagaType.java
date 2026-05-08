package com.example.library.rental.domain.model.saga;

/**
 * rental-service 가 시작한 비동기 업무 흐름의 종류입니다.
 */
public enum RentalSagaType {
    RENT,
    RETURN,
    OVERDUE
}
