package com.example.library.book.application.inputport;

import com.example.library.book.application.outputport.BookOutPutPort;
import com.example.library.book.application.usecase.AddBookUsecase;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookDesc;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddBookInputPort implements AddBookUsecase {
    private final BookOutPutPort bookOutPutPort;

    public AddBookInputPort(BookOutPutPort bookOutPutPort) {
        this.bookOutPutPort = bookOutPutPort;
    }

    @Override
    public Book addBook(String title, BookDesc desc, Classfication classfication, Location location) {
        return bookOutPutPort.save(Book.enterBook(title, desc, classfication, location));
    }
}
