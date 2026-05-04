package com.example.library.book.application.dto;

import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.vo.BookDesc;

/**
 * 도서 등록 업무에 필요한 제목, 설명, 분류, 위치를 담은 application command입니다.
 *
 * @param title 등록하거나 응답할 도서 제목입니다.
 * @param desc 등록할 도서 상세 설명 값 객체입니다.
 * @param classfication 등록하거나 저장할 도서 분류입니다.
 * @param location 등록하거나 저장할 도서 소장 지점입니다.
 */
public record AddBookCommand(String title, BookDesc desc, Classfication classfication, Location location) {
}
