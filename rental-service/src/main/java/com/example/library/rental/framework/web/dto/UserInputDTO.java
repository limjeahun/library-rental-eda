package com.example.library.rental.framework.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class UserInputDTO {
    @NotBlank
    @JsonAlias({"UserId", "userId"})
    private String userId;

    @NotBlank
    @JsonAlias({"UserNm", "userNm"})
    private String userNm;

    public UserInputDTO() {
    }

    public IDName toIdName() {
        return new IDName(userId, userNm);
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
