package com.example.library.book.application.inputport;

import com.example.library.book.application.outputport.BookOutPutPort;
import com.example.library.book.application.usecase.MakeAvailableUsecase;
import com.example.library.book.domain.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MakeAvailableInputPort implements MakeAvailableUsecase {
    private final BookOutPutPort bookOutPutPort;

    public MakeAvailableInputPort(BookOutPutPort bookOutPutPort) {
        this.bookOutPutPort = bookOutPutPort;
    }

    @Override
    public Book makeAvailable(long bookNo) {
        Book book = bookOutPutPort.loadBook(bookNo);
        book.makeAvailable();
        return bookOutPutPort.save(book);
    }
}
