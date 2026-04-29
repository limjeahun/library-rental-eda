package com.example.library.book.application.inputport;

import com.example.library.book.application.outputport.BookOutPutPort;
import com.example.library.book.application.usecase.MakeUnAvailableUsecase;
import com.example.library.book.domain.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MakeUnAvailableInputPort implements MakeUnAvailableUsecase {
    private final BookOutPutPort bookOutPutPort;

    public MakeUnAvailableInputPort(BookOutPutPort bookOutPutPort) {
        this.bookOutPutPort = bookOutPutPort;
    }

    @Override
    public Book makeUnAvailable(long bookNo) {
        Book book = bookOutPutPort.loadBook(bookNo);
        book.makeUnAvailable();
        return bookOutPutPort.save(book);
    }
}
