package com.example.library.rental.application.dto;

import com.example.library.common.event.PointUseReason;
import com.example.library.rental.domain.vo.RentalMember;

/**
 * 회원 서비스에 포인트 차감을 요청하기 위한 application command입니다.
 */
public record PointUseCommandRequest(
    String correlationId,
    RentalMember idName,
    long point,
    PointUseReason reason
) {
}
