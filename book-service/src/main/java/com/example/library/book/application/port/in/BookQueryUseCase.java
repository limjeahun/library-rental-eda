package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.BookResult;

/**
 * 도서 번호로 등록된 도서 정보를 조회하는 application 계약입니다.
 */
public interface BookQueryUseCase {
    /**
     * 도서 번호로 도서 정보를 조회합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return 도서 번호에 해당하는 도서 결과 DTO를 반환합니다.
     */
    BookResult getBook(long bookNo);
}
