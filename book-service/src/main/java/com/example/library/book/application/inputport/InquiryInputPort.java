package com.example.library.book.application.inputport;

import com.example.library.book.application.outputport.BookOutPutPort;
import com.example.library.book.application.usecase.InquiryUsecase;
import com.example.library.book.domain.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InquiryInputPort implements InquiryUsecase {
    private final BookOutPutPort bookOutPutPort;

    public InquiryInputPort(BookOutPutPort bookOutPutPort) {
        this.bookOutPutPort = bookOutPutPort;
    }

    @Override
    public Book getBook(long bookNo) {
        return bookOutPutPort.loadBook(bookNo);
    }
}
