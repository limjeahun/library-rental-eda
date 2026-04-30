package com.example.library.book.domain.model;

import java.time.LocalDate;

public class BookDesc {
    private String description;
    private String author;
    private String isbn;
    private LocalDate publicationDate;
    private Source source;

    public BookDesc(String description, String author, String isbn, LocalDate publicationDate, Source source) {
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publicationDate = publicationDate;
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public Source getSource() {
        return source;
    }
}
