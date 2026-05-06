package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.ReturnItemResult;
import java.time.LocalDate;

/**
 * 반납 완료된 도서 항목을 API 응답으로 표현하는 DTO입니다.
 *
 * @param itemId 대여, 반납, 연체 처리 대상 도서 번호입니다.
 * @param itemTitle 대여 또는 반납 항목의 도서 제목입니다.
 * @param rentDate 저장하거나 복원할 대여일입니다.
 * @param returnDate 도서가 실제로 반납된 날짜입니다.
 */
public record ReturnItemResponse(Long itemId, String itemTitle, LocalDate rentDate, LocalDate returnDate) {
    /**
     * 반납 항목 도메인 모델을 HTTP 응답 DTO로 변환합니다.
     *
     * @param returnItem 변환할 반납 항목 도메인 모델입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static ReturnItemResponse from(ReturnItemResult returnItem) {
        return new ReturnItemResponse(
            returnItem.itemId(),
            returnItem.itemTitle(),
            returnItem.rentDate(),
            returnItem.returnDate()
        );
    }
}
