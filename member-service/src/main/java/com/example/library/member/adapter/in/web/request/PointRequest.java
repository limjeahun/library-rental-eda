package com.example.library.member.adapter.in.web.request;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * 회원 포인트 변경 HTTP 요청을 표현하는 HTTP DTO입니다.
 *
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record PointRequest(@PositiveOrZero long point) {
}
