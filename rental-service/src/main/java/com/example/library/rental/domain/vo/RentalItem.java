package com.example.library.rental.domain.vo;

/**
 * rental-service 내부에서 대여/반납 대상 도서를 표현하는 값 객체입니다.
 */
public record RentalItem(Long no, String title) {
    public RentalItem {
        if (no == null) {
            throw new IllegalArgumentException("도서 번호는 비어 있을 수 없습니다.");
        }
    }
}
