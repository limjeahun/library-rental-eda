package com.example.library.member.application.service;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.Participant;
import com.example.library.common.event.PointUseCommand;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 관련 이벤트와 포인트 command를 처리해 회원 포인트를 변경하는 application service입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemberEventService implements HandleMemberEventUseCase {
    private final SavePointUseCase savePointUseCase;
    private final UsePointUseCase usePointUseCase;
    private final PublishMemberEventResultPort publishMemberEventResultPort;
    private final MessageIdempotencyPort messageIdempotencyPort;

    /**
     * 대여 완료 이벤트를 처리해 회원에게 대여 포인트를 적립합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    @Transactional
    public void handleRent(ItemRented event) {
        if (!messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RENTED
        )) {
            log.info("skip already processed member rent eventId={}", event.eventId());
            return;
        }
        try {
            savePointUseCase.savePoint(
                    new ChangePointCommand(event.memberId(), event.memberName(), event.point())
            );
            publishMemberEventResultPort.publish(result(event, EventType.RENT, SagaStep.MEMBER_SAVE_POINT, true, null));
        } catch (Exception ex) {
            log.error("member rent point save failed eventId={}", event.eventId(), ex);
            publishMemberEventResultPort.publish(result(event, EventType.RENT, SagaStep.MEMBER_SAVE_POINT, false, ex.getMessage()));
        }
    }

    /**
     * 반납 완료 이벤트를 처리해 회원에게 반납 포인트를 적립합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    @Transactional
    public void handleReturn(ItemReturned event) {
        if (!messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RETURNED
        )) {
            log.info("skip already processed member return eventId={}", event.eventId());
            return;
        }
        try {
            savePointUseCase.savePoint(new ChangePointCommand(event.memberId(), event.memberName(), event.point()));
            publishMemberEventResultPort.publish(EventResult.success(
                event.eventId(),
                event.correlationId(),
                EventType.RETURN,
                Participant.MEMBER,
                SagaStep.MEMBER_SAVE_POINT,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point()
            ));
        } catch (Exception ex) {
            log.error("member return point save failed eventId={}", event.eventId(), ex);
            publishMemberEventResultPort.publish(EventResult.failure(
                event.eventId(),
                event.correlationId(),
                EventType.RETURN,
                Participant.MEMBER,
                SagaStep.MEMBER_SAVE_POINT,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point(),
                ex.getMessage()
            ));
        }
    }

    /**
     * 연체 해제 이벤트를 처리해 포인트를 차감하고 처리 결과를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    @Transactional
    public void handleOverdueClear(OverdueCleared event) {
        if (!messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.OVERDUE_CLEARED
        )) {
            log.info("skip already processed overdue clear eventId={}", event.eventId());
            return;
        }
        try {
            usePointUseCase.usePoint(new ChangePointCommand(event.memberId(), event.memberName(), event.point()));
            publishMemberEventResultPort.publish(result(event, true, null));
        } catch (Exception ex) {
            log.error("overdue clear failed eventId={}", event.eventId(), ex);
            publishMemberEventResultPort.publish(result(event, false, ex.getMessage()));
        }
    }

    /**
     * 보상 흐름에서 전달된 포인트 사용 command를 처리합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    @Override
    @Transactional
    public void handlePointUse(PointUseCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.POINT_USE_COMMAND
        )) {
            log.info("skip already processed point_use eventId={}", command.eventId());
            return;
        }
        try {
            usePointUseCase.usePoint(
                    new ChangePointCommand(command.memberId(), command.memberName(), command.point())
            );
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
     * @return 연체 해제 포인트 차감 성공/실패, 회원, 포인트, 사유를 담은 EventResult를 반환합니다.
     */
    private EventResult result(OverdueCleared event, boolean successed, String reason) {
        if (successed) {
            return EventResult.success(
                event.eventId(),
                event.correlationId(),
                EventType.OVERDUE,
                Participant.MEMBER,
                SagaStep.MEMBER_USE_POINT,
                event.memberId(),
                event.memberName(),
                null,
                null,
                event.point()
            );
        }
        return EventResult.failure(
            event.eventId(),
            event.correlationId(),
            EventType.OVERDUE,
            Participant.MEMBER,
            SagaStep.MEMBER_USE_POINT,
            event.memberId(),
            event.memberName(),
            null,
            null,
            event.point(),
            reason
        );
    }

    private EventResult result(ItemRented event, EventType eventType, SagaStep step, boolean successed, String reason) {
        if (successed) {
            return EventResult.success(
                event.eventId(),
                event.correlationId(),
                eventType,
                Participant.MEMBER,
                step,
                event.memberId(),
                event.memberName(),
                event.itemNo(),
                event.itemTitle(),
                event.point()
            );
        }
        return EventResult.failure(
            event.eventId(),
            event.correlationId(),
            eventType,
            Participant.MEMBER,
            step,
            event.memberId(),
            event.memberName(),
            event.itemNo(),
            event.itemTitle(),
            event.point(),
            reason
        );
    }

}
