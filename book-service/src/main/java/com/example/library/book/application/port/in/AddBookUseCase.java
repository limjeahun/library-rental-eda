package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.application.dto.BookResult;

/**
 * 도서 제목, 설명, 분류, 위치를 받아 새 도서를 등록하는 application 계약입니다.
 */
public interface AddBookUseCase {
    /**
     * 도서 등록 command를 처리하고 등록된 도서 결과를 반환합니다.
     *
     * @param command 등록할 도서 제목, 설명, 분류, 위치를 담은 application command입니다.
     * @return 등록된 도서의 번호, 제목, 설명, 분류, 상태, 위치를 담은 결과 DTO를 반환합니다.
     */
    BookResult addBook(AddBookCommand command);
}
