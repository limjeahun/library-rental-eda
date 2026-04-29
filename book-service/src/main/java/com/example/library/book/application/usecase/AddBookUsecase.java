package com.example.library.book.application.usecase;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookDesc;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;

public interface AddBookUsecase {
    Book addBook(String title, BookDesc desc, Classfication classfication, Location location);
}
