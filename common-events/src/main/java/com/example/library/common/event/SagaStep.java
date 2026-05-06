package com.example.library.common.event;

/**
 * 결과 이벤트가 어느 참여 단계의 처리 결과인지 나타내는 공유 프로토콜 enum입니다.
 */
public enum SagaStep {
    BOOK_MAKE_UNAVAILABLE,
    BOOK_MAKE_AVAILABLE,
    MEMBER_SAVE_POINT,
    MEMBER_USE_POINT
}
