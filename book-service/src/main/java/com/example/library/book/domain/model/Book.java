package com.example.library.book.domain.model;

public class Book {
    private Long no;
    private String title;
    private BookDesc desc;
    private Classfication classfication;
    private BookStatus bookStatus;
    private Location location;

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

    public String getTitle() {
        return title;
    }

    public BookDesc getDesc() {
        return desc;
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
