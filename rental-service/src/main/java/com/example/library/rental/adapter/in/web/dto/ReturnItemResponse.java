package com.example.library.rental.adapter.in.web.dto;

import com.example.library.rental.domain.model.ReturnItem;
import java.time.LocalDate;

public record ReturnItemResponse(Long itemId, String itemTitle, LocalDate rentDate, LocalDate returnDate) {
    public static ReturnItemResponse from(ReturnItem returnItem) {
        return new ReturnItemResponse(
            returnItem.getItem().getItem().getNo(),
            returnItem.getItem().getItem().getTitle(),
            returnItem.getItem().getRentDate(),
            returnItem.getReturnDate()
        );
    }
}
