package com.example.library.rental.application.dto;

/**
 * 대여카드 생성 use case의 입력 command입니다.
 */
public record CreateRentalCardCommand(
    String userId,
    String userNm
) {
}
