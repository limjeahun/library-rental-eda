package com.example.library.book.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    private String title;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "description", column = @Column(name = "description")),
        @AttributeOverride(name = "author", column = @Column(name = "author")),
        @AttributeOverride(name = "isbn", column = @Column(name = "isbn")),
        @AttributeOverride(name = "publicationDate", column = @Column(name = "publication_date")),
        @AttributeOverride(name = "source", column = @Column(name = "source"))
    })
    private BookDesc desc;

    @Enumerated(EnumType.STRING)
    private Classfication classfication;

    @Enumerated(EnumType.STRING)
    private BookStatus bookStatus;

    @Enumerated(EnumType.STRING)
    private Location location;

    public Book() {
    }

    public Book(Long no, String title, BookDesc desc, Classfication classfication, BookStatus bookStatus, Location location) {
        this.no = no;
        this.title = title;
        this.desc = desc;
        this.classfication = classfication;
        this.bookStatus = bookStatus;
        this.location = location;
    }

    public static Book enterBook(String title, BookDesc desc, Classfication classfication, Location location) {
        return new Book(null, title, desc, classfication, BookStatus.ENTERED, location);
    }

    public Book makeAvailable() {
        this.bookStatus = BookStatus.AVAILABLE;
        return this;
    }

    public Book makeUnAvailable() {
        this.bookStatus = BookStatus.UNAVAILABLE;
        return this;
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

    public BookDesc getDesc() {
        return desc;
    }

    public void setDesc(BookDesc desc) {
        this.desc = desc;
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
