package com.example.library.book.application.service;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.application.dto.BookResult;
import com.example.library.book.application.port.in.AddBookUseCase;
import com.example.library.book.application.port.in.BookQueryUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.book.application.port.out.BookOutputPort;
import com.example.library.book.domain.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService implements AddBookUseCase, BookQueryUseCase, MakeAvailableBookUseCase, MakeUnavailableBookUseCase {
    private final BookOutputPort bookOutputPort;

    public BookService(BookOutputPort bookOutputPort) {
        this.bookOutputPort = bookOutputPort;
    }

    @Override
    public BookResult addBook(AddBookCommand command) {
        Book book = Book.enterBook(command.title(), command.desc(), command.classfication(), command.location());
        return BookResult.from(bookOutputPort.save(book));
    }

    @Override
    @Transactional(readOnly = true)
    public BookResult getBook(long bookNo) {
        return BookResult.from(bookOutputPort.loadBook(bookNo));
    }

    @Override
    public BookResult makeAvailable(long bookNo) {
        Book book = bookOutputPort.loadBook(bookNo);
        book.makeAvailable();
        return BookResult.from(bookOutputPort.save(book));
    }

    @Override
    public BookResult makeUnavailable(long bookNo) {
        Book book = bookOutputPort.loadBook(bookNo);
        book.makeUnAvailable();
        return BookResult.from(bookOutputPort.save(book));
    }
}
