package com.example.library.book.domain.model;

import com.example.library.book.domain.vo.BookDesc;

/**
 * 도서의 입고 상태, 대여 가능 상태, 위치를 관리하는 순수 도메인 모델입니다.
 */
public class Book {
    private Long no;
    private String title;
    private BookDesc desc;
    private Classfication classfication;
    private BookStatus bookStatus;
    private Location location;

    /**
     * 영속성 어댑터가 저장된 도서 상태를 도메인 모델로 복원할 때 사용합니다.
     *
     * @param no 도서 번호입니다.
     * @param title 등록하거나 응답할 도서 제목입니다.
     * @param desc 등록할 도서 상세 설명 값 객체입니다.
     * @param classfication 등록하거나 저장할 도서 분류입니다.
     * @param bookStatus 저장하거나 복원할 도서 상태입니다.
     * @param location 등록하거나 저장할 도서 소장 지점입니다.
     */
    public Book(Long no, String title, BookDesc desc, Classfication classfication, BookStatus bookStatus, Location location) {
        this.no = no;
        this.title = title;
        this.desc = desc;
        this.classfication = classfication;
        this.bookStatus = bookStatus;
        this.location = location;
    }

    /**
     * 새 도서를 입고 상태로 등록합니다.
     *
     * @param title 등록하거나 응답할 도서 제목입니다.
     * @param desc 등록할 도서 상세 설명 값 객체입니다.
     * @param classfication 등록하거나 저장할 도서 분류입니다.
     * @param location 등록하거나 저장할 도서 소장 지점입니다.
     * @return ENTERED 상태로 생성된 새 도서 도메인 모델을 반환합니다.
     */
    public static Book enterBook(String title, BookDesc desc, Classfication classfication, Location location) {
        return new Book(null, title, desc, classfication, BookStatus.ENTERED, location);
    }

    /**
     * 도서를 대여 가능한 상태로 변경합니다.
     *
     * @return AVAILABLE 상태로 변경된 현재 도서 도메인 모델을 반환합니다.
     */
    public Book makeAvailable() {
        this.bookStatus = BookStatus.AVAILABLE;
        return this;
    }

    /**
     * 도서를 대여 불가능한 상태로 변경합니다.
     *
     * @return UNAVAILABLE 상태로 변경된 현재 도서 도메인 모델을 반환합니다.
     */
    public Book makeUnAvailable() {
        this.bookStatus = BookStatus.UNAVAILABLE;
        return this;
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
     * 도서 상세 설명 값을 반환합니다.
     *
     * @return 도서 설명, 저자, ISBN, 출판일, 출처를 담은 설명 값을 반환합니다.
     */
    public BookDesc getDesc() {
        return desc;
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
     * 도서의 현재 대여 가능 상태를 반환합니다.
     *
     * @return 도서의 AVAILABLE 또는 UNAVAILABLE 상태를 반환합니다.
     */
    public BookStatus getBookStatus() {
        return bookStatus;
    }

    /**
     * 도서가 소장된 지점을 반환합니다.
     *
     * @return 도서가 비치된 위치를 반환합니다.
     */
    public Location getLocation() {
        return location;
    }
}
