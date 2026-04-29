package com.example.library.book.application.usecase;

import com.example.library.book.domain.model.Book;

public interface MakeUnAvailableUsecase {
    Book makeUnAvailable(long bookNo);
}
