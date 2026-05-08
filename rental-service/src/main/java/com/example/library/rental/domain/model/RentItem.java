package com.example.library.rental.domain.model;

import com.example.library.rental.domain.model.policy.RentalPeriodPolicy;
import com.example.library.rental.domain.vo.RentalItem;
import java.time.LocalDate;
import java.util.Objects;

/**
 *  대여 중인 단일 도서와 대여 상태.
 */
public record RentItem(RentalItem item, LocalDate rentDate, boolean overdue, LocalDate overdueDate) {
    public static RentItem createRentalItem(RentalItem item) {
        LocalDate now = LocalDate.now();
        return new RentItem(item, now, false, RentalPeriodPolicy.STANDARD.overdueDateFrom(now));
    }

    public boolean isSameItem(RentalItem other) {
        return item != null && other != null && Objects.equals(item.no(), other.no());
    }

    public RentItem markOverdue() {
        return new RentItem(item, rentDate, true, overdueDate);
    }

    public RentItem withOverdueDate(LocalDate date) {
        return new RentItem(item, rentDate, overdue, date);
    }
}
