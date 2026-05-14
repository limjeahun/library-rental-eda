package com.example.library.member.application.service;

import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.MemberEventResult;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointUseCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.application.port.out.LoadMemberByIdNamePort;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.application.port.out.SaveMemberPort;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.vo.MemberIdentity;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 관련 이벤트와 포인트 command 를 처리해 회원 포인트를 변경하는 application service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemberEventService implements HandleMemberEventUseCase {
    private final PublishMemberEventResultPort publishMemberEventResultPort;
    private final MessageIdempotencyPort messageIdempotencyPort;
    private final LoadMemberByIdNamePort loadMemberByIdNamePort;
    private final SaveMemberPort saveMemberPort;

    /**
     * 대여 완료 이벤트를 처리해 회원에게 대여 포인트를 적립합니다.
     *
     * @param command 처리할 대여 포인트 적립 application command입니다.
     */
    @Override
    @Transactional
    public void handleRent(MemberPointSaveCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENTED
        )) {
            log.info("skip already processed member rent eventId={}", command.eventId());
            return;
        }
        try {
            savePoint(command.memberId(), command.memberName(), command.point());
            publishMemberEventResultPort.publish(result(command, EventType.RENT, SagaStep.MEMBER_SAVE_POINT, true, null));
        } catch (Exception ex) {
            log.error("member rent point save failed eventId={}", command.eventId(), ex);
            publishMemberEventResultPort.publish(result(command, EventType.RENT, SagaStep.MEMBER_SAVE_POINT, false, ex.getMessage()));
        }
    }

    /**
     * 반납 완료 이벤트를 처리해 회원에게 반납 포인트를 적립합니다.
     *
     * @param command 처리할 반납 포인트 적립 application command입니다.
     */
    @Override
    @Transactional
    public void handleReturn(MemberPointSaveCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )) {
            log.info("skip already processed member return eventId={}", command.eventId());
            return;
        }
        try {
            savePoint(command.memberId(), command.memberName(), command.point());
            publishMemberEventResultPort.publish(result(command, EventType.RETURN, SagaStep.MEMBER_SAVE_POINT, true, null));
        } catch (Exception ex) {
            log.error("member return point save failed eventId={}", command.eventId(), ex);
            publishMemberEventResultPort.publish(result(command, EventType.RETURN, SagaStep.MEMBER_SAVE_POINT, false, ex.getMessage()));
        }
    }

    /**
     * 연체 해제 이벤트를 처리해 포인트를 차감하고 처리 결과를 발행합니다.
     *
     * @param command 처리할 연체 해제 포인트 차감 application command입니다.
     */
    @Override
    @Transactional
    public void handleOverdueClear(MemberOverdueClearCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.OVERDUE_CLEARED
        )) {
            log.info("skip already processed overdue clear eventId={}", command.eventId());
            return;
        }
        try {
            usePoint(command.memberId(), command.memberName(), command.point());
            publishMemberEventResultPort.publish(result(command, true, null));
        } catch (Exception ex) {
            log.error("overdue clear failed eventId={}", command.eventId(), ex);
            publishMemberEventResultPort.publish(result(command, false, ex.getMessage()));
        }
    }

    /**
     * 보상 흐름에서 전달된 포인트 사용 command를 처리합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    @Override
    @Transactional
    public void handlePointUse(MemberPointUseCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.POINT_USE_COMMAND
        )) {
            log.info("skip already processed point_use eventId={}", command.eventId());
            return;
        }
        try {
            usePoint(command.memberId(), command.memberName(), command.point());
        } catch (Exception ex) {
            log.error("point_use command failed eventId={} reason={}", command.eventId(), command.reason(), ex);
        }
    }

    /**
     * 대여 서비스가 연체 해제 보상 여부를 판단할 수 있도록 결과 이벤트를 생성합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     * @param successed 참여 서비스 처리 성공 여부입니다.
     * @param reason 실패 결과나 보상 command의 사유입니다.
     * @return 연체 해제 포인트 차감 성공/실패, 회원, 포인트, 사유를 담은 application result를 반환합니다.
     */
    private MemberEventResult result(MemberOverdueClearCommand command, boolean successed, String reason) {
        return new MemberEventResult(
            command.eventId(),
            command.correlationId(),
            EventType.OVERDUE,
            Participant.MEMBER,
            SagaStep.MEMBER_USE_POINT,
            successed,
            command.memberId(),
            command.memberName(),
            null,
            null,
            command.point(),
            reason
        );
    }

    private MemberEventResult result(
        MemberPointSaveCommand command,
        EventType eventType,
        SagaStep step,
        boolean successed,
        String reason
    ) {
        return new MemberEventResult(
            command.eventId(),
            command.correlationId(),
            eventType,
            Participant.MEMBER,
            step,
            successed,
            command.memberId(),
            command.memberName(),
            command.itemNo(),
            command.itemTitle(),
            command.point(),
            reason
        );
    }

    private void savePoint(String memberId, String memberName, long point) {
        Member member = loadMember(memberId, memberName);
        member.savePoint(point);
        saveMemberPort.saveMember(member);
    }

    private void usePoint(String memberId, String memberName, long point) {
        Member member = loadMember(memberId, memberName);
        member.usePoint(point);
        saveMemberPort.saveMember(member);
    }

    private Member loadMember(String memberId, String memberName) {
        return loadMemberByIdNamePort.loadMemberByIdName(new MemberIdentity(memberId, memberName))
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + memberId));
    }

}
