package com.example.library.member.application.port.out;

import com.example.library.member.application.dto.MemberEventResult;

/**
 * 회원 서비스의 비동기 이벤트 처리 결과를 발행하는 outbound port입니다.
 */
public interface PublishMemberEventResultPort {
    /**
     * 회원 서비스의 비동기 이벤트 처리 결과를 발행합니다.
     *
     * @param result 회원 서비스의 이벤트 처리 application result입니다.
     */
    void publish(MemberEventResult result);
}
