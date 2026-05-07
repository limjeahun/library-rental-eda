package com.example.library.rental.domain.model;

/**
 * correlationId 기준으로 한 번만 실행해야 하는 rental-service 보상 종류입니다.
 */
public enum RentalCompensationType {
    RENT_CANCEL,
    RETURN_CANCEL,
    OVERDUE_CLEAR_CANCEL,
    RENT_POINT_USE,
    RETURN_POINT_USE
}
