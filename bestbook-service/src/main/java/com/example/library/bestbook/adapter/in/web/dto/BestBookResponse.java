package com.example.library.bestbook.adapter.in.web.dto;

import com.example.library.bestbook.application.dto.BestBookResult;

public record BestBookResponse(Long id, Long itemNo, String itemTitle, long rentCount) {
    public static BestBookResponse from(BestBookResult result) {
        return new BestBookResponse(result.id(), result.itemNo(), result.itemTitle(), result.rentCount());
    }
}
