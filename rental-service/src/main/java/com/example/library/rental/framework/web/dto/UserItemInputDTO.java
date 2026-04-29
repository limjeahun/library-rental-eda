package com.example.library.rental.framework.web.dto;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserItemInputDTO {
    @NotNull
    @JsonAlias({"itemId", "ItemId"})
    private Long itemId;

    @NotBlank
    @JsonAlias({"itemTitle", "ItemTitle"})
    private String itemTitle;

    @NotBlank
    @JsonAlias({"userId", "UserId"})
    private String userId;

    @NotBlank
    @JsonAlias({"userNm", "UserNm"})
    private String userNm;

    public UserItemInputDTO() {
    }

    public IDName toIdName() {
        return new IDName(userId, userNm);
    }

    public Item toItem() {
        return new Item(itemId, itemTitle);
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNm() {
        return userNm;
    }

    public void setUserNm(String userNm) {
        this.userNm = userNm;
    }
}
