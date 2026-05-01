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

/**
 * 도서 도메인 모델을 MariaDB에 저장하기 위한 JPA 엔티티입니다.
 */
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

    /**
     * JPA가 엔티티를 생성할 때 사용하는 기본 생성자입니다.
     */
    protected BookJpaEntity() {
    }

    /**
     * 저장소 어댑터가 도메인 모델의 현재 상태를 JPA 엔티티로 옮길 때 사용합니다.
     *
     * @param no 도서 번호입니다.
     * @param title 등록하거나 응답할 도서 제목입니다.
     * @param description 등록하거나 저장할 도서 설명입니다.
     * @param author 등록하거나 저장할 도서 저자입니다.
     * @param isbn 등록하거나 저장할 도서 ISBN입니다.
     * @param publicationDate 등록하거나 저장할 도서 발행일입니다.
     * @param source 등록하거나 저장할 도서 입수 경로입니다.
     * @param classfication 등록하거나 저장할 도서 분류입니다.
     * @param bookStatus 저장하거나 복원할 도서 상태입니다.
     * @param location 등록하거나 저장할 도서 소장 지점입니다.
     */
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

    /**
     * 도서 번호를 반환합니다.
     *
     * @return 도서 번호를 반환합니다.
     */
    public Long getNo() {
        return no;
    }

    /**
     * 도서 제목을 반환합니다.
     *
     * @return 도서 제목을 반환합니다.
     */
    public String getTitle() {
        return title;
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

    /**
     * 도서 분류를 반환합니다.
     *
     * @return 도서 분류 값을 반환합니다.
     */
    public Classfication getClassfication() {
        return classfication;
    }

    /**
     * 도서 상태를 반환합니다.
     *
     * @return 도서의 AVAILABLE 또는 UNAVAILABLE 상태를 반환합니다.
     */
    public BookStatus getBookStatus() {
        return bookStatus;
    }

    /**
     * 도서 소장 지점을 반환합니다.
     *
     * @return 도서가 비치된 위치를 반환합니다.
     */
    public Location getLocation() {
        return location;
    }
}
