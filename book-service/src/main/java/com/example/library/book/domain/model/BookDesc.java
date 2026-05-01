package com.example.library.book.domain.model;

import java.time.LocalDate;

/**
 * 도서 설명, 저자, ISBN, 발행일, 입수 경로를 묶는 도서 상세 값 객체입니다.
 */
public class BookDesc {
    private String description;
    private String author;
    private String isbn;
    private LocalDate publicationDate;
    private Source source;

    /**
     * 도서 상세 정보를 생성합니다.
     *
     * @param description 등록하거나 저장할 도서 설명입니다.
     * @param author 등록하거나 저장할 도서 저자입니다.
     * @param isbn 등록하거나 저장할 도서 ISBN입니다.
     * @param publicationDate 등록하거나 저장할 도서 발행일입니다.
     * @param source 등록하거나 저장할 도서 입수 경로입니다.
     */
    public BookDesc(String description, String author, String isbn, LocalDate publicationDate, Source source) {
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publicationDate = publicationDate;
        this.source = source;
    }

    /**
     * 도서 설명을 반환합니다.
     *
     * @return 도서 설명을 반환합니다.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 저자를 반환합니다.
     *
     * @return 도서 저자를 반환합니다.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * ISBN을 반환합니다.
     *
     * @return 도서 ISBN을 반환합니다.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * 발행일을 반환합니다.
     *
     * @return 도서 출판일을 반환합니다.
     */
    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    /**
     * 도서 입수 경로를 반환합니다.
     *
     * @return 도서 입수 출처를 반환합니다.
     */
    public Source getSource() {
        return source;
    }
}
