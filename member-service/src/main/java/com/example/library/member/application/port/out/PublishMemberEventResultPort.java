package com.example.library.member.application.port.out;

import com.example.library.common.event.EventResult;

/**
 * 회원 서비스의 비동기 이벤트 처리 결과를 발행하는 outbound port입니다.
 */
public interface PublishMemberEventResultPort {
    /**
     * 회원 서비스의 비동기 이벤트 처리 결과를 발행합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    void publish(EventResult result);
}
