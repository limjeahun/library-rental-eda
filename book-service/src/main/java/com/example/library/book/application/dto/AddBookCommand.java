package com.example.library.book.application.dto;

import java.time.LocalDate;

/**
 * 도서 등록 업무에 필요한 입력 값을 담은 application command입니다.
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
public record AddBookCommand(
    String title,
    String description,
    String author,
    String isbn,
    LocalDate publicationDate,
    String source,
    String classification,
    String location
) {
}
