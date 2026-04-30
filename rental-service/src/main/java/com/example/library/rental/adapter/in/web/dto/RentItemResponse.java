package com.example.library.rental.adapter.in.web.dto;

import com.example.library.rental.domain.model.RentItem;
import java.time.LocalDate;

public record RentItemResponse(Long itemId, String itemTitle, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
    public static RentItemResponse from(RentItem rentItem) {
        return new RentItemResponse(
            rentItem.getItem().getNo(),
            rentItem.getItem().getTitle(),
            rentItem.getRentDate(),
            rentItem.isOverdued(),
            rentItem.getOverdueDate()
        );
    }
}
