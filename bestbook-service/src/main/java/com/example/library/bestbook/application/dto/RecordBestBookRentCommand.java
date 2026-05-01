package com.example.library.bestbook.application.dto;

/**
 * 도서 대여 이벤트를 인기 도서 read model에 반영하기 위한 application command입니다.
 *
 * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
 */
public record RecordBestBookRentCommand(Long itemNo, String itemTitle) {
}
