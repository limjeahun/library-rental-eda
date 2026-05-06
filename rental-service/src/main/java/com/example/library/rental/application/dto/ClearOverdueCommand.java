package com.example.library.rental.application.dto;

/**
 * 연체 해제 use case의 입력 command입니다.
 */
public record ClearOverdueCommand(
    String userId,
    String userNm,
    long point
) {
}
