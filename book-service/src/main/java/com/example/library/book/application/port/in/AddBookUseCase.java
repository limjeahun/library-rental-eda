package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.application.dto.BookResult;

public interface AddBookUseCase {
    BookResult addBook(AddBookCommand command);
}
