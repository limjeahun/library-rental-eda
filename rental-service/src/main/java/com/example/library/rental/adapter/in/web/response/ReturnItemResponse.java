package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.ReturnItemResult;
import java.time.LocalDate;
import java.util.List;

/**
 * 반납 완료된 도서 항목 응답 DTO.
 *
 * @param itemId 반납 완료 도서 번호.
 * @param itemTitle 반납 완료 도서 제목.
 * @param rentDate 대여일.
 * @param returnDate 실제 반납일.
 */
public record ReturnItemResponse(Long itemId, String itemTitle, LocalDate rentDate, LocalDate returnDate) {
    /**
     * 반납 항목 결과를 응답 DTO로 옮깁니다.
     *
     * @param returnItem 반납 항목 application 결과.
     * @return 클라이언트에 반환할 응답 DTO.
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
     * 반납 항목 결과 목록을 응답 DTO 목록으로 옮깁니다.
     *
     * @param returnItems 반납 완료 도서 항목 결과 목록.
     * @return 반납 항목 응답 DTO 목록.
     */
    public static List<ReturnItemResponse> from(List<ReturnItemResult> returnItems) {
        return returnItems.stream()
                .map(ReturnItemResponse::from)
                .toList();
    }


}
