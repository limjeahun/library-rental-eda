package com.example.library.book.adapter.in.web.dto;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.domain.model.BookDesc;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String author,
    @NotBlank String isbn,
    @NotNull LocalDate publicationDate,
    @NotNull Source source,
    @NotNull Classfication classfication,
    @NotNull Location location
) {
    public AddBookCommand toCommand() {
        return new AddBookCommand(
            title,
            new BookDesc(description, author, isbn, publicationDate, source),
            classfication,
            location
        );
    }
}
