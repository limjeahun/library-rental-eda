package com.example.library.book.adapter.in.web.dto;

import com.example.library.book.application.dto.BookResult;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

/**
 * 도서 API 응답으로 반환하는 HTTP DTO입니다.
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
public record BookResponse(
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
     * application 결과 DTO를 HTTP 응답 DTO로 변환합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static BookResponse from(BookResult result) {
        return new BookResponse(
            result.no(),
            result.title(),
            result.description(),
            result.author(),
            result.isbn(),
            result.publicationDate(),
            result.source(),
            result.classfication(),
            result.bookStatus(),
            result.location()
        );
    }
}
