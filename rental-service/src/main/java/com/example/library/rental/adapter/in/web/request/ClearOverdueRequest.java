package com.example.library.rental.adapter.in.web.request;

import com.example.library.rental.domain.vo.RentalMember;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 연체 해제 요청 DTO.
 *
 * @param userId 대여카드 소유자를 식별하는 회원 ID.
 * @param userNm 대여카드 요청 또는 응답에서 사용할 회원 이름.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
 */
public record ClearOverdueRequest(
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm,
    @PositiveOrZero long point
) {
    /**
     * 요청의 회원 정보를 공통 회원 식별 값으로 변환.
     *
     * @return 요청 회원 ID와 이름을 담은 공통 값 객체를 반환.
     */
    public RentalMember toIdName() {
        return new RentalMember(userId, userNm);
    }
}
