package com.example.library.member.application.dto;

/**
 * 대여/반납 이벤트에 따른 회원 포인트 적립 application command입니다.
 */
public record MemberPointSaveCommand(
    String eventId,
    String correlationId,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point
) {
}
