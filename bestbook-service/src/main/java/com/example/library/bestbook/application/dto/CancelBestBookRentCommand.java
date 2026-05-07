package com.example.library.bestbook.application.dto;

import com.example.library.common.event.InboundMessageType;

/**
 * 대여 보상 완료 이벤트를 인기 도서 read model에 반영하기 위한 application command입니다.
 */
public record CancelBestBookRentCommand(
    Long itemNo,
    String eventId,
    String correlationId,
    InboundMessageType messageType
) {
}
