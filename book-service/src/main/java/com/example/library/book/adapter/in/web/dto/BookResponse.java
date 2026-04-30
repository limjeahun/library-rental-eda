package com.example.library.book.adapter.in.web.dto;

import com.example.library.book.application.dto.BookResult;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

public record BookResponse(
    Long no,
    String title,
    String description,
    String author,
    String isbn,
    LocalDate publicationDate,
    Source source,
    Classfication classfication,
    BookStatus bookStatus,
    Location location
) {
    public static BookResponse from(BookResult result) {
        return new BookResponse(
            result.no(),
            result.title(),
            result.description(),
            result.author(),
            result.isbn(),
            result.publicationDate(),
            result.source(),
            result.classfication(),
            result.bookStatus(),
            result.location()
        );
    }
}
