package com.example.library.book.adapter.in.web.request;

import com.example.library.book.application.dto.AddBookCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 도서 등록 HTTP 요청을 표현하는 HTTP DTO입니다.
 *
 * @param title 도서 제목.
 * @param description 도서 설명.
 * @param author 도서 저자.
 * @param isbn 도서 ISBN.
 * @param publicationDate 도서 발행일.
 * @param source 도서 입수 경로.
 * @param classification 도서 분류.
 * @param location 도서 소장 지점.
 */
public record BookRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String author,
    @NotBlank String isbn,
    @NotNull LocalDate publicationDate,
    @NotBlank String source,
    @NotBlank String classification,
    @NotBlank String location
) {
    /**
     * web 요청 값을 도서 등록 command로 넘깁니다.
     *
     * @return 요청 값을 담은 application command.
     */
    public AddBookCommand toCommand() {
        return new AddBookCommand(
            title,
            description,
            author,
            isbn,
            publicationDate,
            source,
            classification,
            location
        );
    }
}
