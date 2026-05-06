package com.example.library.rental.application.dto;

import com.example.library.rental.domain.model.RentItem;
import java.time.LocalDate;

/**
 * 대여 중인 도서 항목을 application 계층 밖으로 반환하기 위한 result DTO입니다.
 */
public record RentItemResult(
    Long itemId,
    String itemTitle,
    LocalDate rentDate,
    boolean overdued,
    LocalDate overdueDate
) {
    public static RentItemResult from(RentItem rentItem) {
        return new RentItemResult(
            rentItem.item().no(),
            rentItem.item().title(),
            rentItem.rentDate(),
            rentItem.overdued(),
            rentItem.overdueDate()
        );
    }
}
