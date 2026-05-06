package com.example.library.rental.application.dto;

/**
 * 도서 대여 use case의 입력 command입니다.
 */
public record RentItemCommand(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
}
