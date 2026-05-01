package com.example.library.rental.adapter.in.web.dto;

/**
 * 대여/반납/연체 해제 요청 후 결과 메시지와 최신 대여카드를 함께 반환하는 DTO입니다.
 *
 * @param message 클라이언트에 전달할 처리 안내 메시지입니다.
 * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
 */
public record RentalResultResponse(String message, RentalCardResponse rentalCard) {
    /**
     * 사용자 안내 메시지와 대여카드 응답을 묶어 결과 DTO를 생성합니다.
     *
     * @param message 클라이언트에 전달할 처리 안내 메시지입니다.
     * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static RentalResultResponse of(String message, RentalCardResponse rentalCard) {
        return new RentalResultResponse(message, rentalCard);
    }
}
