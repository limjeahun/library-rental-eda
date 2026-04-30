package com.example.library.rental.adapter.in.web.dto;

import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.domain.model.RentalCard;
import java.util.List;

public record RentalCardResponse(
    String rentalCardNo,
    String userId,
    String userNm,
    RentStatus rentStatus,
    long lateFee,
    List<RentItemResponse> rentItems,
    List<ReturnItemResponse> returnItems
) {
    public static RentalCardResponse from(RentalCard rentalCard) {
        return new RentalCardResponse(
            rentalCard.getRentalCardNo(),
            rentalCard.getMember().getId(),
            rentalCard.getMember().getName(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().getPoint(),
            rentalCard.getRentItemList().stream().map(RentItemResponse::from).toList(),
            rentalCard.getReturnItemList().stream().map(ReturnItemResponse::from).toList()
        );
    }
}
