package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import java.time.Instant;

/**
 * 대여 서비스가 회원 서비스에 포인트 차감을 요청하는 command 메시지입니다.
 */
public record PointUseCommand(
    String eventId,
    String correlationId,
    Instant occurredAt,
    IDName idName,
    long point,
    String reason
) {
}
