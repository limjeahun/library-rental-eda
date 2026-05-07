package com.example.library.rental.domain.model;

/**
 * rental-service가 시작한 비동기 업무 흐름의 종류입니다.
 */
public enum RentalSagaType {
    RENT,
    RETURN,
    OVERDUE
}
