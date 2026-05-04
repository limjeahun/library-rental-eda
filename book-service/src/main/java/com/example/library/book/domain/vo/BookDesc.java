package com.example.library.book.domain.vo;

import com.example.library.book.domain.model.Source;
import java.time.LocalDate;

/**
 * 도서 설명, 저자, ISBN, 발행일, 입수 경로를 묶는 도서 상세 값 객체입니다.
 */
public record BookDesc(
    String description,
    String author,
    String isbn,
    LocalDate publicationDate,
    Source source
) {
}
