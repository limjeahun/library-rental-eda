package com.example.library.rental.application.dto;

/**
 * 도서 반납 use case의 입력 command입니다.
 */
public record ReturnItemCommand(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
}
