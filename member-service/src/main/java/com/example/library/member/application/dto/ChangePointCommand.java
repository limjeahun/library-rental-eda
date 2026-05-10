package com.example.library.member.application.dto;

/**
 * 회원 포인트 적립 또는 차감에 필요한 회원 식별 값과 포인트 금액을 담은 application command입니다.
 *
 * @param memberId 대상 회원 ID입니다.
 * @param memberName 대상 회원 이름입니다.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record ChangePointCommand(String memberId, String memberName, long point) {
}
