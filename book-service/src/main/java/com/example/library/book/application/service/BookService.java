package com.example.library.book.application.service;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.application.dto.BookResult;
import com.example.library.book.application.port.in.AddBookUseCase;
import com.example.library.book.application.port.in.BookQueryUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.book.application.port.out.BookOutputPort;
import com.example.library.book.domain.model.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서 등록, 조회, AVAILABLE/UNAVAILABLE 상태 변경 흐름을 조율하는 application service입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BookService implements AddBookUseCase, BookQueryUseCase, MakeAvailableBookUseCase, MakeUnavailableBookUseCase {
    private final BookOutputPort bookOutputPort;

    /**
     * 새 도서를 입고 상태로 등록하고 저장 결과를 반환합니다.
     *
     * @param command 등록할 도서 제목, 설명, 분류, 위치를 담은 application command입니다.
     * @return 등록된 도서의 번호, 제목, 설명, 분류, 상태, 위치를 담은 결과 DTO를 반환합니다.
     */
    @Override
    public BookResult addBook(AddBookCommand command) {
        Book book = Book.enterBook(command.title(), command.desc(), command.classfication(), command.location());
        return BookResult.from(bookOutputPort.save(book));
    }

    /**
     * 도서 번호로 도서를 조회합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return 도서 번호에 해당하는 도서 결과 DTO를 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public BookResult getBook(long bookNo) {
        return BookResult.from(bookOutputPort.loadBook(bookNo));
    }

    /**
     * 도서를 대여 가능한 상태로 변경합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return AVAILABLE 상태가 반영된 도서 번호, 제목, 설명, 분류, 위치 결과를 반환합니다.
     */
    @Override
    public BookResult makeAvailable(long bookNo) {
        Book book = bookOutputPort.loadBook(bookNo);
        book.makeAvailable();
        return BookResult.from(bookOutputPort.save(book));
    }

    /**
     * 도서를 대여 불가능한 상태로 변경합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return UNAVAILABLE 상태가 반영된 도서 번호, 제목, 설명, 분류, 위치 결과를 반환합니다.
     */
    @Override
    public BookResult makeUnavailable(long bookNo) {
        Book book = bookOutputPort.loadBook(bookNo);
        book.makeUnAvailable();
        return BookResult.from(bookOutputPort.save(book));
    }
}
