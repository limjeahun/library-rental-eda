package com.example.library.rental.domain.model;

import java.time.LocalDate;

/**
 * RentalCard aggregate 안에서 반납 완료된 대여 항목과 실제 반납일을 표현하는 불변 child model입니다.
 */
public record ReturnItem(RentItem item, LocalDate returnDate) {
    public static ReturnItem createReturnItem(RentItem item) {
        return new ReturnItem(item, LocalDate.now());
    }

    public static ReturnItem createReturnItem(RentItem item, LocalDate returnDate) {
        return new ReturnItem(item, returnDate);
    }
}
