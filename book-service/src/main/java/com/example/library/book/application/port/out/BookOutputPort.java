package com.example.library.book.application.port.out;

import com.example.library.book.domain.model.Book;

public interface BookOutputPort {
    Book save(Book book);

    Book loadBook(long bookNo);
}
