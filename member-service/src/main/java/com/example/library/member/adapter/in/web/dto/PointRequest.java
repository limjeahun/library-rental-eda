package com.example.library.member.adapter.in.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record PointRequest(@PositiveOrZero long point) {
}
