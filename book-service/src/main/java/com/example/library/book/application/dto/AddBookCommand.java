package com.example.library.book.application.dto;

import java.time.LocalDate;

/**
 * 도서 등록 업무에 필요한 입력 값을 담은 application command입니다.
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
public record AddBookCommand(
    String title,
    String description,
    String author,
    String isbn,
    LocalDate publicationDate,
    String source,
    String classfication,
    String location
) {
}
