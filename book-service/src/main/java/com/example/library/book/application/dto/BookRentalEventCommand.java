package com.example.library.book.application.dto;

/**
 * 대여/반납 이벤트를 처리하기 위한 도서 서비스 application command입니다.
 */
public record BookRentalEventCommand(
    String eventId,
    String correlationId,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point
) {

}
