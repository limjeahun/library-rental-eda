package com.example.library.member.application.dto;

import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;

/**
 * 회원 서비스의 비동기 이벤트 처리 결과를 표현하는 application result입니다.
 */
public record MemberEventResult(
    String sourceEventId,
    String correlationId,
    EventType eventType,
    Participant participant,
    SagaStep step,
    boolean successed,
    String memberId,
    String memberName,
    Long itemNo,
    String itemTitle,
    long point,
    String reason
) {
}
