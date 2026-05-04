package com.example.library.rental.domain.vo;

import java.time.Year;
import java.util.UUID;

/**
 * 대여카드 번호를 표현하는 값 객체입니다.
 */
public record RentalCardNo(String no) {
    public static RentalCardNo createRentalCardNo() {
        return new RentalCardNo(Year.now().getValue() + "-" + UUID.randomUUID());
    }
}
