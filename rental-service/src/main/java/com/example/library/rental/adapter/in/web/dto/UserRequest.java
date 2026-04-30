package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
    @NotBlank @JsonAlias({"UserId", "userId"}) String userId,
    @NotBlank @JsonAlias({"UserNm", "userNm"}) String userNm
) {
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }
}
