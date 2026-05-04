package com.example.library.rental.domain.model;

import com.example.library.common.vo.Item;
import java.time.LocalDate;
import java.util.Objects;

/**
 *  대여 중인 단일 도서와 대여 상태.
 */
public record RentItem(Item item, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
    private static final int RENTAL_DAYS = 14;

    public static RentItem createRentalItem(Item item) {
        LocalDate now = LocalDate.now();
        return new RentItem(item, now, false, now.plusDays(RENTAL_DAYS));
    }

    public boolean isSameItem(Item other) {
        return item != null && other != null && Objects.equals(item.no(), other.no());
    }

    public RentItem markOverdued() {
        return new RentItem(item, rentDate, true, overdueDate);
    }

    public RentItem withOverdueDate(LocalDate date) {
        return new RentItem(item, rentDate, overdued, date);
    }
}
