package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.BookResult;

public interface MakeAvailableBookUseCase {
    BookResult makeAvailable(long bookNo);
}
