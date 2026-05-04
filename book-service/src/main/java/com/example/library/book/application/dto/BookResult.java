package com.example.library.book.application.dto;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

/**
 * 도메인 도서를 외부 계층에 노출하기 위해 application 계층에서 사용하는 결과 DTO입니다.
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
public record BookResult(
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
    /**
     * 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 도메인 모델 또는 application 결과 DTO를 HTTP 응답 DTO로 변환해 반환합니다.
     */
    public static BookResult from(Book book) {
        return new BookResult(
            book.getNo(),
            book.getTitle(),
            book.getDesc().description(),
            book.getDesc().author(),
            book.getDesc().isbn(),
            book.getDesc().publicationDate(),
            book.getDesc().source(),
            book.getClassfication(),
            book.getBookStatus(),
            book.getLocation()
        );
    }
}
