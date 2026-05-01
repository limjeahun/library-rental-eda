package com.example.library.book.application.port.out;

import com.example.library.book.domain.model.Book;

/**
 * 도서 번호로 도메인 도서를 조회하는 outbound port입니다.
 */
public interface LoadBookPort {
    /**
     * 도서 번호로 도메인 도서를 조회합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return 도서 번호에 해당하는 도서 도메인 모델을 반환합니다.
     */
    Book loadBook(long bookNo);
}
