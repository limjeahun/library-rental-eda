package com.example.library.rental.application.dto;

import com.example.library.rental.domain.model.ReturnItem;
import java.time.LocalDate;

/**
 * 반납 완료 도서 항목을 application 계층 밖으로 반환하기 위한 result DTO입니다.
 */
public record ReturnItemResult(
    Long itemId,
    String itemTitle,
    LocalDate rentDate,
    LocalDate returnDate
) {
    public static ReturnItemResult from(ReturnItem returnItem) {
        return new ReturnItemResult(
            returnItem.item().item().no(),
            returnItem.item().item().title(),
            returnItem.item().rentDate(),
            returnItem.returnDate()
        );
    }
}
