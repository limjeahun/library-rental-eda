package com.example.library.member.framework.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public class PointRequestDTO {
    @PositiveOrZero
    private long point;

    public PointRequestDTO() {
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
