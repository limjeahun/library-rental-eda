package com.example.library.rental.domain.model;

import java.time.LocalDate;

/**
 * 대여일 기준 반납 기한을 계산하는 정책입니다.
 */
public enum RentalPeriodPolicy {
    STANDARD(14);

    private final long rentalDays;

    RentalPeriodPolicy(long rentalDays) {
        this.rentalDays = rentalDays;
    }

    public LocalDate overdueDateFrom(LocalDate rentDate) {
        return rentDate.plusDays(rentalDays);
    }
}
