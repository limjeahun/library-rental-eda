package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.RentalCardResult;
import com.example.library.rental.domain.model.RentStatus;
import java.util.List;

/**
 * 요청 접수 메시지와 최신 대여카드 상태를 함께 반환하는 HTTP DTO.
 *
 * @param message 클라이언트에 전달할 처리 안내 메시지.
 * @param rentalCardNo 대여카드 번호.
 * @param userId 대여카드 소유 회원 ID.
 * @param userNm 대여카드 소유 회원 이름.
 * @param rentStatus 대여카드 상태.
 * @param lateFee 연체료 포인트.
 * @param rentItems 대여 중 도서 목록.
 * @param returnItems 반납 완료 도서 목록.
 */
public record RentalResultResponse(
        String message,
        String rentalCardNo,
        String userId,
        String userNm,
        RentStatus rentStatus,
        long lateFee,
        List<RentItemResponse> rentItems,
        List<ReturnItemResponse> returnItems
) {
    public RentalResultResponse {
        rentItems = List.copyOf(rentItems);
        returnItems = List.copyOf(returnItems);
    }

    @Override
    public List<RentItemResponse> rentItems() {
        return List.copyOf(rentItems);
    }

    @Override
    public List<ReturnItemResponse> returnItems() {
        return List.copyOf(returnItems);
    }

    /**
     * 도서 대여 요청 접수 응답을 만듭니다.
     *
     * @param rentalCard 도서 대여 처리 후 반환된 최신 대여카드 결과.
     * @return 클라이언트에 반환할 도서 대여 요청 접수 응답 DTO.
     */
    public static RentalResultResponse rentAccepted(RentalCardResult rentalCard) {
        return from("도서 대여 요청을 접수했습니다.", rentalCard);
    }

    /**
     * 도서 반납 요청 접수 응답을 만듭니다.
     *
     * @param rentalCard 도서 반납 처리 후 반환된 최신 대여카드 결과.
     * @return 클라이언트에 반환할 도서 반납 요청 접수 응답 DTO.
     */
    public static RentalResultResponse returnAccepted(RentalCardResult rentalCard) {
        return from("도서 반납 요청을 접수했습니다.", rentalCard);
    }

    /**
     * 연체료 정산 요청 접수 응답을 만듭니다.
     *
     * @param rentalCard 연체료 정산 처리 후 반환된 최신 대여카드 결과.
     * @return 클라이언트에 반환할 연체료 정산 요청 접수 응답 DTO.
     */
    public static RentalResultResponse clearOverdueAccepted(RentalCardResult rentalCard) {
        return from("연체료 정산 요청을 접수했습니다.", rentalCard);
    }

    /**
     * 같은 응답 형태에 접수 메시지만 다르게 넣습니다.
     *
     * @param message 클라이언트에 전달할 처리 안내 메시지.
     * @param rentalCard 응답 본문에 포함할 최신 대여카드 결과.
     * @return 클라이언트에 반환할 요청 처리 응답 DTO.
     */
    private static RentalResultResponse from(String message, RentalCardResult rentalCard) {
        return new RentalResultResponse(
                message,
                rentalCard.rentalCardNo(),
                rentalCard.userId(),
                rentalCard.userName(),
                rentalCard.rentStatus(),
                rentalCard.lateFee(),
                RentItemResponse.from(rentalCard.rentItems()),
                ReturnItemResponse.from(rentalCard.returnItems())
        );
    }
}
