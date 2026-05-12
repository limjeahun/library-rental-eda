package com.example.library.book.domain.model;

/**
 * 도서 등록과 조회에서 사용하는 도서 분류입니다.
 */
public enum Classification {
    ARTS,
    COMPUTER,
    LITERATURE;

    public static Classification from(String name) {
        return Classification.valueOf(name);
    }

}
