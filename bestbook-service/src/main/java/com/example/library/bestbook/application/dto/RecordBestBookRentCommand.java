package com.example.library.bestbook.application.dto;

import com.example.library.common.event.InboundMessageType;

/**
 * 도서 대여 이벤트를 인기 도서 read model에 반영하기 위한 application command.
 *
 * @param itemNo 인기 도서 집계 대상 도서 번호.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
 * @param eventId 처리 완료 멱등성 판단에 사용할 메시지 식별자.
 * @param correlationId 하나의 비동기 업무 흐름을 묶는 상관관계 식별자.
 * @param messageType 처리 완료 기록에 저장할 메시지 타입.
 */
public record RecordBestBookRentCommand(
    Long itemNo,
    String itemTitle,
    String eventId,
    String correlationId,
    InboundMessageType messageType
) {

}
