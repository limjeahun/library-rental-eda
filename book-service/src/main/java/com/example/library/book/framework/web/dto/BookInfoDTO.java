package com.example.library.book.framework.web.dto;

import com.example.library.book.domain.model.BookDesc;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BookInfoDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String author;

    @NotBlank
    private String isbn;

    @NotNull
    private LocalDate publicationDate;

    @NotNull
    private Source source;

    @NotNull
    private Classfication classfication;

    @NotNull
    private Location location;

    public BookInfoDTO() {
    }

    public BookDesc toBookDesc() {
        return new BookDesc(description, author, isbn, publicationDate, source);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Classfication getClassfication() {
        return classfication;
    }

    public void setClassfication(Classfication classfication) {
        this.classfication = classfication;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
