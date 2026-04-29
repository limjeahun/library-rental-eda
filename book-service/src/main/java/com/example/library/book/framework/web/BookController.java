package com.example.library.book.framework.web;

import com.example.library.book.application.usecase.AddBookUsecase;
import com.example.library.book.application.usecase.InquiryUsecase;
import com.example.library.book.application.usecase.MakeAvailableUsecase;
import com.example.library.book.application.usecase.MakeUnAvailableUsecase;
import com.example.library.book.framework.web.dto.BookInfoDTO;
import com.example.library.book.framework.web.dto.BookOutPutDTO;
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
    private final AddBookUsecase addBookUsecase;
    private final InquiryUsecase inquiryUsecase;
    private final MakeAvailableUsecase makeAvailableUsecase;
    private final MakeUnAvailableUsecase makeUnAvailableUsecase;

    public BookController(
        AddBookUsecase addBookUsecase,
        InquiryUsecase inquiryUsecase,
        MakeAvailableUsecase makeAvailableUsecase,
        MakeUnAvailableUsecase makeUnAvailableUsecase
    ) {
        this.addBookUsecase = addBookUsecase;
        this.inquiryUsecase = inquiryUsecase;
        this.makeAvailableUsecase = makeAvailableUsecase;
        this.makeUnAvailableUsecase = makeUnAvailableUsecase;
    }

    @PostMapping
    public BookOutPutDTO addBook(@Valid @RequestBody BookInfoDTO bookInfoDTO) {
        return BookOutPutDTO.from(addBookUsecase.addBook(
            bookInfoDTO.getTitle(),
            bookInfoDTO.toBookDesc(),
            bookInfoDTO.getClassfication(),
            bookInfoDTO.getLocation()
        ));
    }

    @GetMapping("/{no}")
    public BookOutPutDTO getBook(@PathVariable long no) {
        return BookOutPutDTO.from(inquiryUsecase.getBook(no));
    }

    @PostMapping("/{no}/available")
    public BookOutPutDTO makeAvailable(@PathVariable long no) {
        return BookOutPutDTO.from(makeAvailableUsecase.makeAvailable(no));
    }

    @PostMapping("/{no}/unavailable")
    public BookOutPutDTO makeUnAvailable(@PathVariable long no) {
        return BookOutPutDTO.from(makeUnAvailableUsecase.makeUnAvailable(no));
    }
}
