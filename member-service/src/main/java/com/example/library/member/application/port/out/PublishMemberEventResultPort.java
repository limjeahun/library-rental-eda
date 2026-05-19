package com.example.library.member.application.port.out;

import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointSaveResultContext;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;

/**
 * 회원 서비스의 비동기 이벤트 처리 결과를 발행하는 outbound port 입니다.
 */
public interface PublishMemberEventResultPort {
    /**
     * 대여 포인트 적립 성공 결과를 발행합니다.
     *
     * @param event 회원 aggregate 가 발생시킨 포인트 적립 도메인 이벤트입니다.
     * @param context 이벤트 결과 정보
     */
    void publishRentPointSaved(
        MemberPointSavedDomainEvent event,
        MemberPointSaveResultContext context
    );

    void publishRentPointSaveFailed(MemberPointSaveCommand command, String reason);

    void publishReturnPointSaved(
        MemberPointSavedDomainEvent event,
        MemberPointSaveResultContext context
    );

    void publishReturnPointSaveFailed(MemberPointSaveCommand command, String reason);

    void publishOverduePointUsed(
        MemberPointUsedDomainEvent event,
        String sourceEventId,
        String correlationId
    );

    void publishOverduePointUseFailed(MemberOverdueClearCommand command, String reason);
}
