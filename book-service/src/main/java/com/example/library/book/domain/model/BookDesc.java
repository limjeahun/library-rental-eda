package com.example.library.book.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

@Embeddable
public class BookDesc {
    private String description;
    private String author;
    private String isbn;
    private LocalDate publicationDate;

    @Enumerated(EnumType.STRING)
    private Source source;

    public BookDesc() {
    }

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

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
