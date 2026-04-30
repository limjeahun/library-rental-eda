package com.example.library.bestbook.application.dto;

import com.example.library.bestbook.domain.model.BestBook;

public record BestBookResult(Long id, Long itemNo, String itemTitle, long rentCount) {
    public static BestBookResult from(BestBook bestBook) {
        return new BestBookResult(
            bestBook.getId(),
            bestBook.getItemNo(),
            bestBook.getItemTitle(),
            bestBook.getRentCount()
        );
    }
}
