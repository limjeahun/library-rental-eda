package com.example.library.rental.adapter.in.web.request;

import com.example.library.rental.application.dto.RentItemCommand;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 도서 대여 HTTP 요청 DTO.
 *
 * @param itemId 대여 대상 도서 번호.
 * @param itemTitle 대여 대상 도서 제목.
 * @param userId 대여카드 소유 회원 ID.
 * @param userNm 대여카드 소유 회원 이름.
 */
public record RentItemRequest(
    @NotNull @JsonAlias({"itemId", "ItemId"}) Long itemId,
    @NotBlank @JsonAlias({"itemTitle", "ItemTitle"}) String itemTitle,
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm
) {
    /**
     * web 요청 값을 도서 대여 command로 넘깁니다.
     *
     * @return 요청 회원과 도서 정보를 담은 application command.
     */
    public RentItemCommand toCommand() {
        return new RentItemCommand(userId, userNm, itemId, itemTitle);
    }
}
