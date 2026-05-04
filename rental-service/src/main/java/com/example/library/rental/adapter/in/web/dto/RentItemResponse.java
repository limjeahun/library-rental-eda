package com.example.library.rental.adapter.in.web.dto;

import com.example.library.rental.domain.model.RentItem;
import java.time.LocalDate;

/**
 * 대여 중인 도서 항목을 API 응답으로 표현하는 DTO입니다.
 *
 * @param itemId 대여, 반납, 연체 처리 대상 도서 번호입니다.
 * @param itemTitle 대여 또는 반납 항목의 도서 제목입니다.
 * @param rentDate 저장하거나 복원할 대여일입니다.
 * @param overdued 저장하거나 복원할 연체 여부입니다.
 * @param overdueDate 저장하거나 복원할 반납 예정일입니다.
 */
public record RentItemResponse(Long itemId, String itemTitle, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
    /**
     * 대여 항목 도메인 모델을 HTTP 응답 DTO로 변환합니다.
     *
     * @param rentItem 반납 또는 연체료 계산 대상 대여 항목입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static RentItemResponse from(RentItem rentItem) {
        return new RentItemResponse(
            rentItem.item().no(),
            rentItem.item().title(),
            rentItem.rentDate(),
            rentItem.overdued(),
            rentItem.overdueDate()
        );
    }
}
