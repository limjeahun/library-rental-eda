package com.example.library.book.adapter.in.web.dto;

import com.example.library.book.application.dto.AddBookCommand;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import com.example.library.book.domain.vo.BookDesc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 도서 등록 HTTP 요청을 표현하는 HTTP DTO입니다.
 *
 * @param title 등록하거나 응답할 도서 제목입니다.
 * @param description 등록하거나 저장할 도서 설명입니다.
 * @param author 등록하거나 저장할 도서 저자입니다.
 * @param isbn 등록하거나 저장할 도서 ISBN입니다.
 * @param publicationDate 등록하거나 저장할 도서 발행일입니다.
 * @param source 등록하거나 저장할 도서 입수 경로입니다.
 * @param classfication 등록하거나 저장할 도서 분류입니다.
 * @param location 등록하거나 저장할 도서 소장 지점입니다.
 */
public record BookRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String author,
    @NotBlank String isbn,
    @NotNull LocalDate publicationDate,
    @NotNull Source source,
    @NotNull Classfication classfication,
    @NotNull Location location
) {
    /**
     * web 요청 DTO를 도서 등록 application command로 변환합니다.
     *
     * @return 요청 값을 업무 처리에 사용할 application command로 변환해 반환합니다.
     */
    public AddBookCommand toCommand() {
        return new AddBookCommand(
            title,
            new BookDesc(description, author, isbn, publicationDate, source),
            classfication,
            location
        );
    }
}
