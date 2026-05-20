package com.example.library.book.adapter.in.web.response;

import com.example.library.book.application.dto.BookResult;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classification;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import java.time.LocalDate;
import java.util.List;

/**
 * 도서 API 응답으로 반환하는 HTTP DTO입니다.
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
public record BookResponse(
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
            result.classification(),
            result.bookStatus(),
            result.location()
        );
    }

    /**
     * application 결과 DTO 목록을 HTTP 응답 DTO 목록으로 변환합니다.
     *
     * @param results 클라이언트에 반환할 도서 결과 목록입니다.
     * @return 클라이언트에 반환할 도서 HTTP 응답 DTO 목록을 반환합니다.
     */
    public static List<BookResponse> from(List<BookResult> results) {
        return results.stream()
            .map(BookResponse::from)
            .toList();
    }
}
