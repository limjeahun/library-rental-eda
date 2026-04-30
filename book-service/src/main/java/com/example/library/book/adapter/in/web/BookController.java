package com.example.library.book.adapter.in.web;

import com.example.library.book.adapter.in.web.dto.BookRequest;
import com.example.library.book.adapter.in.web.dto.BookResponse;
import com.example.library.book.application.port.in.AddBookUseCase;
import com.example.library.book.application.port.in.BookQueryUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book")
public class BookController {
    private final AddBookUseCase addBookUseCase;
    private final BookQueryUseCase bookQueryUseCase;
    private final MakeAvailableBookUseCase makeAvailableBookUseCase;
    private final MakeUnavailableBookUseCase makeUnavailableBookUseCase;

    public BookController(
        AddBookUseCase addBookUseCase,
        BookQueryUseCase bookQueryUseCase,
        MakeAvailableBookUseCase makeAvailableBookUseCase,
        MakeUnavailableBookUseCase makeUnavailableBookUseCase
    ) {
        this.addBookUseCase = addBookUseCase;
        this.bookQueryUseCase = bookQueryUseCase;
        this.makeAvailableBookUseCase = makeAvailableBookUseCase;
        this.makeUnavailableBookUseCase = makeUnavailableBookUseCase;
    }

    @PostMapping
    public BookResponse addBook(@Valid @RequestBody BookRequest request) {
        return BookResponse.from(addBookUseCase.addBook(request.toCommand()));
    }

    @GetMapping("/{no}")
    public BookResponse getBook(@PathVariable long no) {
        return BookResponse.from(bookQueryUseCase.getBook(no));
    }

    @PostMapping("/{no}/available")
    public BookResponse makeAvailable(@PathVariable long no) {
        return BookResponse.from(makeAvailableBookUseCase.makeAvailable(no));
    }

    @PostMapping("/{no}/unavailable")
    public BookResponse makeUnavailable(@PathVariable long no) {
        return BookResponse.from(makeUnavailableBookUseCase.makeUnavailable(no));
    }
}
