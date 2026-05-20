package com.example.library.book.application.dto;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classification;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

/**
 * 도메인 도서를 외부 계층에 노출하기 위한 application 결과 DTO입니다.
 *
 * @param no 도서 번호.
 * @param title 도서 제목.
 * @param description 도서 설명.
 * @param author 도서 저자.
 * @param isbn 도서 ISBN.
 * @param publicationDate 도서 발행일.
 * @param source 도서 입수 경로.
 * @param classification 도서 분류.
 * @param bookStatus 도서 상태.
 * @param location 도서 소장 지점.
 */
public record BookResult(
    Long no,
    String title,
    String description,
    String author,
    String isbn,
    LocalDate publicationDate,
    Source source,
    Classification classification,
    BookStatus bookStatus,
    Location location
) {
    /**
     * 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param book 변환할 도서 도메인 모델.
     * @return 도서 application 결과 DTO.
     */
    public static BookResult from(Book book) {
        return new BookResult(
            book.no(),
            book.title(),
            book.desc().description(),
            book.desc().author(),
            book.desc().isbn(),
            book.desc().publicationDate(),
            book.desc().source(),
            book.classification(),
            book.bookStatus(),
            book.location()
        );
    }
}
