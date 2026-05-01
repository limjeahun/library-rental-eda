package com.example.library.member.application.port.out;

import com.example.library.common.event.EventResult;

/**
 * 회원 application 서비스가 연체 해제 포인트 차감 성공/실패 결과를 Kafka로 알릴 때 사용하는 발행 계약입니다.
 */
public interface MemberEventOutputPort {
    /**
     * 회원 서비스의 비동기 이벤트 처리 결과를 발행합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    void publish(EventResult result);
}
