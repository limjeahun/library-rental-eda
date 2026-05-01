package com.example.library.book.application.port.out;

import com.example.library.book.domain.model.Book;

/**
 * 도메인 도서를 저장하는 outbound port입니다.
 */
public interface SaveBookPort {
    /**
     * 도메인 도서를 저장하고 저장된 도메인 모델을 반환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    Book save(Book book);
}
