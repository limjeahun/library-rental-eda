package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.application.dto.RentalCardResult;
import java.util.List;

/**
 * 대여카드 API 응답으로 반환하는 HTTP DTO.
 *
 * @param rentalCardNo 대여카드 번호.
 * @param userId 대여카드 소유 회원 ID.
 * @param userNm 대여카드 소유 회원 이름.
 * @param rentStatus 대여카드 상태.
 * @param lateFee 연체료 포인트.
 * @param rentItems 대여 중 도서 목록.
 * @param returnItems 반납 완료 도서 목록.
 */
public record RentalCardResponse(
    String rentalCardNo,
    String userId,
    String userNm,
    RentStatus rentStatus,
    long lateFee,
    List<RentItemResponse> rentItems,
    List<ReturnItemResponse> returnItems
) {
    /**
     * application 결과를 HTTP 응답 형태로 옮깁니다.
     *
     * @param rentalCard 응답 DTO로 변환할 대여카드 application 결과.
     * @return 클라이언트에 반환할 HTTP 응답 DTO.
     */
    public static RentalCardResponse from(RentalCardResult rentalCard) {
        return new RentalCardResponse(
            rentalCard.rentalCardNo(),
            rentalCard.userId(),
            rentalCard.userName(),
            rentalCard.rentStatus(),
            rentalCard.lateFee(),
            rentalCard.rentItems().stream().map(RentItemResponse::from).toList(),
            rentalCard.returnItems().stream().map(ReturnItemResponse::from).toList()
        );
    }
}
