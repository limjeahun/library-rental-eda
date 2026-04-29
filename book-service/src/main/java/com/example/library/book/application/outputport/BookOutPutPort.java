package com.example.library.book.application.outputport;

import com.example.library.book.domain.model.Book;

public interface BookOutPutPort {
    Book loadBook(long bookNo);

    Book save(Book book);
}
