package com.example.library.member.application.dto;

import com.example.library.common.vo.IDName;

/**
 * 회원 포인트 적립 또는 차감에 필요한 회원 식별 값과 포인트 금액을 담은 application command입니다.
 *
 * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record ChangePointCommand(IDName idName, long point) {
}
