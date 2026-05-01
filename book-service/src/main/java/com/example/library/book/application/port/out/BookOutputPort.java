package com.example.library.book.application.port.out;

import com.example.library.book.domain.model.Book;

/**
 * 도서 application 서비스가 도서 도메인 모델의 저장과 조회를 요청할 때 사용하는 저장소 계약입니다.
 */
public interface BookOutputPort {
    /**
     * 도메인 도서를 저장하고 저장된 도메인 모델을 반환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    Book save(Book book);

    /**
     * 도서 번호로 도메인 도서를 조회합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return 도서 번호에 해당하는 도서 도메인 모델을 반환합니다.
     */
    Book loadBook(long bookNo);
}
