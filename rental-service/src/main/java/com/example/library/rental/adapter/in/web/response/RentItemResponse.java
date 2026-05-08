package com.example.library.rental.adapter.in.web.response;

import com.example.library.rental.application.dto.RentItemResult;
import java.time.LocalDate;
import java.util.List;

/**
 * 대여 중인 도서 항목 응답 DTO.
 *
 * @param itemId 대여, 반납, 연체 처리 대상 도서 번호입니다.
 * @param itemTitle 대여 또는 반납 항목의 도서 제목입니다.
 * @param rentDate 저장하거나 복원할 대여일입니다.
 * @param overdue 저장하거나 복원할 연체 여부입니다.
 * @param overdueDate 저장하거나 복원할 반납 예정일입니다.
 */
public record RentItemResponse(Long itemId, String itemTitle, LocalDate rentDate, boolean overdue, LocalDate overdueDate) {
    /**
     * 대여 항목 도메인 모델을 응답 DTO 변환.
     *
     * @param rentItem 반납 또는 연체료 계산 대상 대여 항목.
     * @return 클라이언트에 반환할 응답 DTO 반환.
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
     * 대여 항목 도메인 모델을 응답 DTO 로 변환.
     * @param rentItems 대여 중인 도서 항목 DTO 리스트.
     * @return 대여 항목 반환.
     */
    public static List<RentItemResponse> from(List<RentItemResult> rentItems) {
        return rentItems.stream()
                .map(RentItemResponse::from)
                .toList();
    }

}
