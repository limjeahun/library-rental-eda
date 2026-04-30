package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserItemRequest(
    @NotNull @JsonAlias({"itemId", "ItemId"}) Long itemId,
    @NotBlank @JsonAlias({"itemTitle", "ItemTitle"}) String itemTitle,
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm
) {
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }

    public Item toItem() {
        return new Item(itemId, itemTitle);
    }
}
