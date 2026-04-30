package com.example.library.book.adapter.out.persistence.entity;

import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "books")
public class BookJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String isbn;

    @Column(nullable = false)
    private LocalDate publicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classfication classfication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus bookStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Location location;

    protected BookJpaEntity() {
    }

    public BookJpaEntity(
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

    public Long getNo() {
        return no;
    }

    public String getTitle() {
        return title;
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

    public Classfication getClassfication() {
        return classfication;
    }

    public BookStatus getBookStatus() {
        return bookStatus;
    }

    public Location getLocation() {
        return location;
    }
}
