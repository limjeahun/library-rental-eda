package com.example.library.rental.domain.model.policy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 반납 지연 일수에 따라 연체 포인트를 계산하는 정책입니다.
 */
public enum RentalLateFeePolicy {
    DAILY(10L);

    private final long pointPerDay;

    RentalLateFeePolicy(long pointPerDay) {
        this.pointPerDay = pointPerDay;
    }

    public long calculate(LocalDate overdueDate, LocalDate returnDate) {
        if (returnDate.isAfter(overdueDate)) {
            return ChronoUnit.DAYS.between(overdueDate, returnDate) * pointPerDay;
        }
        return 0;
    }
}
