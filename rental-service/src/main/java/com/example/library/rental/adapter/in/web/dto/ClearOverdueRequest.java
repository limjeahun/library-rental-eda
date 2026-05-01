package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 연체 해제 HTTP 요청을 표현하는 HTTP DTO입니다.
 *
 * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
 * @param userNm 대여카드 요청 또는 응답에서 사용할 회원 이름입니다.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record ClearOverdueRequest(
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm,
    @PositiveOrZero long point
) {
    /**
     * 요청의 회원 정보를 공통 회원 식별 값으로 변환합니다.
     *
     * @return 요청 회원 ID와 이름을 담은 공통 값 객체를 반환합니다.
     */
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }
}
