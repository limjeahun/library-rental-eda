package com.example.library.book.application.dto;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

public record BookResult(
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
    public static BookResult from(Book book) {
        return new BookResult(
            book.getNo(),
            book.getTitle(),
            book.getDesc().getDescription(),
            book.getDesc().getAuthor(),
            book.getDesc().getIsbn(),
            book.getDesc().getPublicationDate(),
            book.getDesc().getSource(),
            book.getClassfication(),
            book.getBookStatus(),
            book.getLocation()
        );
    }
}
