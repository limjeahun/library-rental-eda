package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 회원과 도서가 함께 필요한 대여/반납/연체 HTTP 요청 DTO.
 *
 * @param itemId 대여, 반납, 연체 처리 대상 도서 번호.
 * @param itemTitle 대여 또는 반납 항목의 도서 제목.
 * @param userId 대여카드 소유자를 식별하는 회원 ID.
 * @param userNm 대여카드 요청 또는 응답에서 사용할 회원 이름.
 */
public record UserItemRequest(
    @NotNull @JsonAlias({"itemId", "ItemId"}) Long itemId,
    @NotBlank @JsonAlias({"itemTitle", "ItemTitle"}) String itemTitle,
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm
) {
    /**
     * 요청의 회원 정보를 공통 회원 식별 값으로 변환.
     *
     * @return 요청 회원 ID와 이름을 담은 공통 값 객체를 반환.
     */
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }

    /**
     * 요청의 도서 정보를 공통 도서 값으로 변환.
     *
     * @return 요청 도서 번호와 제목을 담은 공통 값 객체를 반환.
     */
    public Item toItem() {
        return new Item(itemId, itemTitle);
    }
}
