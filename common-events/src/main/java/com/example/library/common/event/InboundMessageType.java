package com.example.library.common.event;

/**
 * 각 서비스가 inbound Kafka 메시지 처리 완료를 기록할 때 사용하는 메시지 종류입니다.
 */
public enum InboundMessageType {
    ITEM_RENTED,
    ITEM_RETURNED,
    OVERDUE_CLEARED,
    POINT_USE_COMMAND,
    EVENT_RESULT,
    ITEM_RENT_CANCELED,
    ITEM_RETURN_CANCELED,
    OVERDUE_CLEAR_CANCELED,
    MANUAL_BESTBOOK_RENT
}
