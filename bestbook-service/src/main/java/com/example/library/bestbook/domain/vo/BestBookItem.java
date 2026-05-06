package com.example.library.bestbook.domain.vo;

/**
 * bestbook-service 내부에서 인기 도서 집계 대상을 표현하는 값 객체입니다.
 */
public record BestBookItem(Long no, String title) {
    public BestBookItem {
        if (no == null) {
            throw new IllegalArgumentException("도서 번호는 비어 있을 수 없습니다.");
        }
    }
}
