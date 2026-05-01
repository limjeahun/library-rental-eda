package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.BookResult;

/**
 * 도서 반납 결과를 반영해 도서를 AVAILABLE 상태로 저장하는 application 계약입니다.
 */
public interface MakeAvailableBookUseCase {
    /**
     * 지정한 도서를 대여 가능 상태로 변경합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return AVAILABLE 상태가 반영된 도서 번호, 제목, 설명, 분류, 위치 결과를 반환합니다.
     */
    BookResult makeAvailable(long bookNo);
}
