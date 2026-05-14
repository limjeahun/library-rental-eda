package com.example.library.bestbook.adapter.in.web.request;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.common.event.InboundMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 인기 도서 대여 기록 HTTP 요청 DTO.
 *
 * @param itemNo 인기 도서 집계 대상 도서 번호.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
 */
public record BestBookRegisterRequest(
    @NotNull Long itemNo,
    @NotBlank String itemTitle
) {
    /**
     * 인기 도서 기록 application command 로 변환.
     *
     * @return 요청 값을 업무 처리에 사용할 application command 로 변환해 반환.
     */
    public RecordBestBookRentCommand toCommand() {
        String eventId = UUID.randomUUID().toString();
        return new RecordBestBookRentCommand(
            itemNo,
            itemTitle,
            eventId,
            eventId,
            InboundMessageType.MANUAL_BESTBOOK_RENT
        );
    }
}
