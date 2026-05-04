package com.example.library.common.vo;

/**
 * 도서 서비스와 대여 서비스가 메시지로 공유하는 도서 식별 값 객체입니다.
 */
public record Item(Long no, String title) {
}
