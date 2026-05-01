package com.example.library.book.domain.model;

/**
 * 도서가 대여 흐름에서 가질 수 있는 상태를 나타냅니다.
 */
public enum BookStatus {
    ENTERED,
    AVAILABLE,
    UNAVAILABLE
}
