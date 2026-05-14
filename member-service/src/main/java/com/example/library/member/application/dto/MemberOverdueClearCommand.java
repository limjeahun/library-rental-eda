package com.example.library.member.application.dto;

/**
 * 연체 해제 이벤트에 따른 회원 포인트 차감 application command입니다.
 */
public record MemberOverdueClearCommand(
    String eventId,
    String correlationId,
    String memberId,
    String memberName,
    long point
) {
}
