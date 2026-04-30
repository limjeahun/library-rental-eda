package com.example.library.bestbook.adapter.in.web.dto;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BestBookRegisterRequest(
    @NotNull Long itemNo,
    @NotBlank String itemTitle
) {
    public RecordBestBookRentCommand toCommand() {
        return new RecordBestBookRentCommand(itemNo, itemTitle);
    }
}
