package com.example.library.book.domain.model;

import com.example.library.book.domain.event.BookDomainEvent;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.book.domain.vo.BookDesc;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * 도서의 입고 상태, 대여 가능 상태, 위치를 관리하는 순수 도메인 모델입니다.
 */
@Getter
public class Book {
    /**
     *  도서 번호.
     */
    private final Long no;
    /**
     *  도서 제목.
     */
    private final String title;
    /**
     *  도서 상세 설명.
     */
    private final BookDesc desc;
    /**
     *  도서 분류.
     */
    private final Classification classification;
    /**
     *  도서의 현재 대여 가능 상태.
     */
    private BookStatus bookStatus;
    /**
     *  도서가 소장된 지점.
     */
    private final Location location;

    /**
     * 현재 aggregate 상태 변경 중 발생한 도메인 이벤트 목록.
     */
    @Getter(AccessLevel.NONE)
    private final List<BookDomainEvent> domainEvents = new ArrayList<>();

    /**
     * 영속성 어댑터가 저장된 도서 상태를 도메인 모델로 복원할 때 사용합니다.
     *
     * @param no 도서 번호.
     * @param title 도서 제목.
     * @param desc 도서 상세 설명.
     * @param classification 도서 분류.
     * @param bookStatus 도서 상태.
     * @param location 도서 소장 지점.
     */
    public Book(Long no, String title, BookDesc desc, Classification classification, BookStatus bookStatus, Location location) {
        this.no = no;
        this.title = title;
        this.desc = desc;
        this.classification = classification;
        this.bookStatus = bookStatus;
        this.location = location;
    }

    /**
     * 새 도서를 입고 상태로 등록합니다.
     *
     * @param title 도서 제목.
     * @param desc 도서 상세 설명.
     * @param classification 도서 분류.
     * @param location 도서 소장 지점.
     * @return ENTERED 새 도서 도메인 모델.
     */
    public static Book enterBook(String title, BookDesc desc, Classification classification, Location location) {
        return new Book(null, title, desc, classification, BookStatus.ENTERED, location);
    }

    /**
     * 도서 모델 재구성
     *
     * @param no 도서 번호.
     * @param title 도서 제목.
     * @param desc 도서 상세 설명.
     * @param classification 도서 분류.
     * @param bookStatus 도서 상태.
     * @param location 도서 소장 지점.
     * @return Book Model
     */
    public static Book reconstitute(
            Long no,
            String title,
            BookDesc desc,
            Classification classification,
            BookStatus bookStatus,
            Location location
    ) {
        return new Book(no, title, desc, classification, bookStatus, location);
    }
    /**
     * 도서를 대여 가능한 상태로 변경합니다.
     *
     * @return AVAILABLE 상태로 변경된 현재 도서 도메인 모델을 반환합니다.
     */
    public Book makeAvailable() {
        this.bookStatus = BookStatus.AVAILABLE;
        registerDomainEvent(new BookMadeAvailableDomainEvent(no, title));
        return this;
    }

    /**
     * 도서를 대여 불가능한 상태로 변경합니다.
     *
     * @return UNAVAILABLE 상태로 변경된 현재 도서 도메인 모델을 반환합니다.
     */
    public Book makeUnAvailable() {
        this.bookStatus = BookStatus.UNAVAILABLE;
        registerDomainEvent(new BookMadeUnavailableDomainEvent(no, title));
        return this;
    }

    private void registerDomainEvent(BookDomainEvent event) {
        domainEvents.add(event);
    }

    public List<BookDomainEvent> pullDomainEvents() {
        List<BookDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

}
