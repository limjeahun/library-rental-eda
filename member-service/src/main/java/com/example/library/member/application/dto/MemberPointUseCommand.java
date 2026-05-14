package com.example.library.member.application.dto;

/**
 * 보상 흐름에서 회원 포인트를 차감하기 위한 application command입니다.
 */
public record MemberPointUseCommand(
    String eventId,
    String correlationId,
    String memberId,
    String memberName,
    long point,
    String reason
) {
}
