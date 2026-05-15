package com.example.library.member.application.port.out;

import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;

/**
 * 회원 서비스의 비동기 이벤트 처리 결과를 발행하는 outbound port입니다.
 */
public interface PublishMemberEventResultPort {
    /**
     * 대여 포인트 적립 성공 결과를 발행합니다.
     *
     * @param event 회원 aggregate 가 발생시킨 포인트 적립 도메인 이벤트입니다.
     * @param sourceEventId 처리한 원본 통합 이벤트 ID입니다.
     * @param correlationId 비동기 흐름을 연결하는 correlation ID입니다.
     * @param itemNo 대여 대상 도서 번호 snapshot 입니다.
     * @param itemTitle 대여 대상 도서 제목 snapshot 입니다.
     */
    void publishRentPointSaved(
        MemberPointSavedDomainEvent event,
        String sourceEventId,
        String correlationId,
        Long itemNo,
        String itemTitle
    );

    void publishRentPointSaveFailed(MemberPointSaveCommand command, String reason);

    void publishReturnPointSaved(
        MemberPointSavedDomainEvent event,
        String sourceEventId,
        String correlationId,
        Long itemNo,
        String itemTitle
    );

    void publishReturnPointSaveFailed(MemberPointSaveCommand command, String reason);

    void publishOverduePointUsed(
        MemberPointUsedDomainEvent event,
        String sourceEventId,
        String correlationId
    );

    void publishOverduePointUseFailed(MemberOverdueClearCommand command, String reason);
}
