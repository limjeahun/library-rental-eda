package com.example.library.rental.domain.vo;

/**
 * rental-service 내부에서 대여카드 소유 회원을 표현하는 값 객체입니다.
 */
public record RentalMember(String id, String name) {
    public RentalMember {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("회원 ID는 비어 있을 수 없습니다.");
        }
    }
}
