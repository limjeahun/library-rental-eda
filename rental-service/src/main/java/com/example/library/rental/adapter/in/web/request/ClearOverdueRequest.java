package com.example.library.rental.adapter.in.web.request;

import com.example.library.rental.application.dto.ClearOverdueCommand;
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
     * 요청의 회원/포인트 정보를 연체 해제 command로 변환.
     *
     * @return 요청 회원과 포인트 정보를 담은 application command를 반환.
     */
    public ClearOverdueCommand toCommand() {
        return new ClearOverdueCommand(userId, userNm, point);
    }
}
