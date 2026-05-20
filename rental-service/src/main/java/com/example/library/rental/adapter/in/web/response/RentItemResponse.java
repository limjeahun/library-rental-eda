package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.RentItemResult;
import java.time.LocalDate;
import java.util.List;

/**
 * 대여 중인 도서 항목 응답 DTO.
 *
 * @param itemId 대여 중인 도서 번호.
 * @param itemTitle 대여 중인 도서 제목.
 * @param rentDate 대여일.
 * @param overdue 연체 여부.
 * @param overdueDate 반납 예정일.
 */
public record RentItemResponse(Long itemId, String itemTitle, LocalDate rentDate, boolean overdue, LocalDate overdueDate) {
    /**
     * 대여 항목 결과를 응답 DTO로 옮깁니다.
     *
     * @param rentItem 대여 항목 application 결과.
     * @return 클라이언트에 반환할 응답 DTO.
     */
    public static RentItemResponse from(RentItemResult rentItem) {
        return new RentItemResponse(
            rentItem.itemId(),
            rentItem.itemTitle(),
            rentItem.rentDate(),
            rentItem.overdue(),
            rentItem.overdueDate()
        );
    }

    /**
     * 대여 항목 결과 목록을 응답 DTO 목록으로 옮깁니다.
     *
     * @param rentItems 대여 중인 도서 항목 결과 목록.
     * @return 대여 항목 응답 DTO 목록.
     */
    public static List<RentItemResponse> from(List<RentItemResult> rentItems) {
        return rentItems.stream()
                .map(RentItemResponse::from)
                .toList();
    }

}
