package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원 식별 정보만 필요한 대여카드 HTTP 요청 DTO.
 *
 * @param userId 대여카드 소유자를 식별하는 회원 ID.
 * @param userNm 대여카드 요청 또는 응답에서 사용할 회원 이름.
 */
public record UserRequest(
    @NotBlank @JsonAlias({"UserId", "userId"}) String userId,
    @NotBlank @JsonAlias({"UserNm", "userNm"}) String userNm
) {
    /**
     * 요청의 회원 정보를 공통 회원 식별 값으로 변환.
     *
     * @return 요청 회원 ID와 이름을 담은 공통 값 객체를 반환.
     */
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }
}
