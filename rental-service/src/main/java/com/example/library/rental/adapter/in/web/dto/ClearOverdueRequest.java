package com.example.library.rental.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ClearOverdueRequest(
    @NotBlank @JsonAlias({"userId", "UserId"}) String userId,
    @NotBlank @JsonAlias({"userNm", "UserNm"}) String userNm,
    @PositiveOrZero long point
) {
    public IDName toIdName() {
        return new IDName(userId, userNm);
    }
}
