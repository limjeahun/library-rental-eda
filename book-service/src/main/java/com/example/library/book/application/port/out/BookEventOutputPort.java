package com.example.library.book.application.port.out;

import com.example.library.common.event.EventResult;

/**
 * 도서 application 서비스가 도서 상태 변경 성공/실패 결과를 Kafka로 알릴 때 사용하는 발행 계약입니다.
 */
public interface BookEventOutputPort {
    /**
     * 도서 서비스의 대여 이벤트 처리 결과를 발행합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    void publish(EventResult result);
}
