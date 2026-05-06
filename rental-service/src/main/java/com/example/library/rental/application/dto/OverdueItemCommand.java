package com.example.library.rental.application.dto;

/**
 * 도서 연체 표시 use case의 입력 command입니다.
 */
public record OverdueItemCommand(
    String userId,
    String userNm,
    Long itemNo,
    String itemTitle
) {
}
