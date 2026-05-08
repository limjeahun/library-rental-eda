package com.example.library.rental.application.dto;

import com.example.library.common.event.PointUseReason;

/**
 * 회원 서비스에 포인트 차감 command를 발행하기 위한 application payload입니다.
 */
public record PointUseCommandPayload(
    String correlationId,
    String memberId,
    String memberName,
    long point,
    PointUseReason reason
) {
}
