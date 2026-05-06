package com.example.library.rental.application.dto;

import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.domain.model.RentalCard;
import java.util.List;

/**
 * 대여카드 조회와 변경 결과를 application 계층 밖으로 반환하기 위한 result DTO입니다.
 */
public record RentalCardResult(
    String rentalCardNo,
    String userId,
    String userName,
    RentStatus rentStatus,
    long lateFee,
    List<RentItemResult> rentItems,
    List<ReturnItemResult> returnItems
) {
    public static RentalCardResult from(RentalCard rentalCard) {
        return new RentalCardResult(
            rentalCard.getRentalCardNo(),
            rentalCard.getMember().id(),
            rentalCard.getMember().name(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().point(),
            rentalCard.getRentItemList().stream().map(RentItemResult::from).toList(),
            rentalCard.getReturnItemList().stream().map(ReturnItemResult::from).toList()
        );
    }
}
