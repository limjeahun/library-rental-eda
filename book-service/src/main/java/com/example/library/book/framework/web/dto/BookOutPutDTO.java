package com.example.library.book.framework.web.dto;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

public class BookOutPutDTO {
    private Long no;
    private String title;
    private String description;
    private String author;
    private String isbn;
    private LocalDate publicationDate;
    private Source source;
    private Classfication classfication;
    private BookStatus bookStatus;
    private Location location;

    public BookOutPutDTO() {
    }

    public BookOutPutDTO(
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
        this.no = no;
        this.title = title;
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publicationDate = publicationDate;
        this.source = source;
        this.classfication = classfication;
        this.bookStatus = bookStatus;
        this.location = location;
    }

    public static BookOutPutDTO from(Book book) {
        return new BookOutPutDTO(
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

    public Long getNo() {
        return no;
    }

    public void setNo(Long no) {
        this.no = no;
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

    public BookStatus getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(BookStatus bookStatus) {
        this.bookStatus = bookStatus;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
