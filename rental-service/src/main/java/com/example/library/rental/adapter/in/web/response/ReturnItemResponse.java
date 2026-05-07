package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.ReturnItemResult;
import java.time.LocalDate;
import java.util.List;

/**
 * 반납 완료된 도서 항목을 API 응답 DTO.
 *
 * @param itemId 대여, 반납, 연체 처리 대상 도서 번호입니다.
 * @param itemTitle 대여 또는 반납 항목의 도서 제목입니다.
 * @param rentDate 저장하거나 복원할 대여일입니다.
 * @param returnDate 도서가 실제로 반납된 날짜입니다.
 */
public record ReturnItemResponse(Long itemId, String itemTitle, LocalDate rentDate, LocalDate returnDate) {
    /**
     * 반납 항목 도메인 모델을 응답 DTO 변환.
     *
     * @param returnItem 변환할 반납 항목 도메인 모델.
     * @return 클라이언트에 반환할 응답 DTO 반환.
     */
    public static ReturnItemResponse from(ReturnItemResult returnItem) {
        return new ReturnItemResponse(
            returnItem.itemId(),
            returnItem.itemTitle(),
            returnItem.rentDate(),
            returnItem.returnDate()
        );
    }

    /**
     * 반납 완료된 도서 항목들을 응답 DTO 변환.
     * @param returnItems 반납 완료 도서 항목.
     * @return 반납 완료된 도서 항목들.
     */
    public static List<ReturnItemResponse> from(List<ReturnItemResult> returnItems) {
        return returnItems.stream()
                .map(ReturnItemResponse::from)
                .toList();
    }


}
