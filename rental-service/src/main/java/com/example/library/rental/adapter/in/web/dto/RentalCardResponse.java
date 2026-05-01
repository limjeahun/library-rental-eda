package com.example.library.rental.adapter.in.web.dto;

import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.domain.model.RentalCard;
import java.util.List;

/**
 * 대여카드 API 응답으로 반환하는 HTTP DTO입니다.
 *
 * @param rentalCardNo 설정할 대여카드 번호입니다.
 * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
 * @param userNm 대여카드 요청 또는 응답에서 사용할 회원 이름입니다.
 * @param rentStatus 저장하거나 설정할 대여카드 상태입니다.
 * @param lateFee 저장하거나 설정할 연체료 값 객체입니다.
 * @param rentItems 저장할 대여 중 도서 값 목록입니다.
 * @param returnItems 저장할 반납 완료 도서 값 목록입니다.
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
     * 대여카드 도메인 모델을 HTTP 응답 DTO로 변환합니다.
     *
     * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
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
