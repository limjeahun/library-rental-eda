package com.example.library.rental.framework.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class ClearOverdueInfoDTO {
    @NotBlank
    @JsonAlias({"userId", "UserId"})
    private String userId;

    @NotBlank
    @JsonAlias({"userNm", "UserNm"})
    private String userNm;

    @PositiveOrZero
    private long point;

    public ClearOverdueInfoDTO() {
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

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
