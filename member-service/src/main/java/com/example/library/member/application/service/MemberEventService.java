package com.example.library.member.application.service;

import com.example.library.common.event.InboundMessageType;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointSaveResultContext;
import com.example.library.member.application.dto.MemberPointUseCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.application.port.out.LoadMemberByIdNamePort;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.application.port.out.SaveMemberPort;
import com.example.library.member.domain.event.MemberDomainEvent;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.vo.MemberIdentity;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 관련 이벤트와 포인트 command 를 처리해 회원 포인트를 변경하는 application service.
 *
 * <p>상태 변경 전에 processed message를 먼저 기록해 Kafka 재전달로 인한 중복 포인트 변경을 막습니다.
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
     * @param command 처리할 대여 포인트 적립 application command 입니다.
     */
    @Override
    @Transactional
    public void handleRent(MemberPointSaveCommand command) {
        handlePointSave(
            command,
            InboundMessageType.ITEM_RENTED,
            "member rent",
            "member rent point save",
            publishMemberEventResultPort::publishRentPointSaved,
            publishMemberEventResultPort::publishRentPointSaveFailed
        );
    }

    /**
     * 반납 완료 이벤트를 처리해 회원에게 반납 포인트를 적립합니다.
     *
     * @param command 처리할 반납 포인트 적립 application command 입니다.
     */
    @Override
    @Transactional
    public void handleReturn(MemberPointSaveCommand command) {
        handlePointSave(
            command,
            InboundMessageType.ITEM_RETURNED,
            "member return",
            "member return point save",
            publishMemberEventResultPort::publishReturnPointSaved,
            publishMemberEventResultPort::publishReturnPointSaveFailed
        );
    }

    /**
     * 연체 해제 이벤트를 처리해 포인트를 차감하고 처리 결과를 발행합니다.
     *
     * @param command 처리할 연체 해제 포인트 차감 application command입니다.
     */
    @Override
    @Transactional
    public void handleOverdueClear(MemberOverdueClearCommand command) {
        processInboundMessage(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.OVERDUE_CLEARED,
            "overdue clear",
            () -> {
                Member member = usePoint(command.memberId(), command.memberName(), command.point());
                var event = pullRequiredEvent(member, MemberPointUsedDomainEvent.class);
                publishMemberEventResultPort.publishOverduePointUsed(
                    event,
                    command.eventId(),
                    command.correlationId()
                );
            },
            ex -> publishFailure(
                "overdue clear",
                command.eventId(),
                ex,
                reason -> publishMemberEventResultPort.publishOverduePointUseFailed(command, reason)
            )
        );
    }

    /**
     * 보상 흐름에서 전달된 포인트 사용 command를 처리합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    @Override
    @Transactional
    public void handlePointUse(MemberPointUseCommand command) {
        processInboundMessage(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.POINT_USE_COMMAND,
            "point_use",
            () -> usePoint(command.memberId(), command.memberName(), command.point()),
            ex -> log.error(
                "point_use command failed eventId={} reason={}",
                command.eventId(),
                command.reason(),
                ex
            )
        );
    }

    private void handlePointSave(
        MemberPointSaveCommand command,
        InboundMessageType messageType,
        String duplicateLogName,
        String failureLogName,
        BiConsumer<MemberPointSavedDomainEvent, MemberPointSaveResultContext> successPublisher,
        BiConsumer<MemberPointSaveCommand, String> failurePublisher
    ) {
        processInboundMessage(
            command.eventId(),
            command.correlationId(),
            messageType,
            duplicateLogName,
            () -> {
                var member = savePoint(command.memberId(), command.memberName(), command.point());
                var event = pullRequiredEvent(member, MemberPointSavedDomainEvent.class);
                var context = MemberPointSaveResultContext.from(command);
                successPublisher.accept(event, context);
            },
            ex -> publishFailure(
                failureLogName,
                command.eventId(),
                ex,
                reason -> failurePublisher.accept(command, reason)
            )
        );
    }

    private void processInboundMessage(
        String eventId,
        String correlationId,
        InboundMessageType messageType,
        String duplicateLogName,
        Runnable handler,
        Consumer<Exception> failureHandler
    ) {
        if (!messageIdempotencyPort.markProcessed(eventId, correlationId, messageType)) {
            log.info("skip already processed {} eventId={}", duplicateLogName, eventId);
            return;
        }
        try {
            handler.run();
        } catch (Exception ex) {
            failureHandler.accept(ex);
        }
    }

    private void publishFailure(
        String failureLogName,
        String eventId,
        Exception ex,
        Consumer<String> failurePublisher
    ) {
        log.error("{} failed eventId={}", failureLogName, eventId, ex);
        failurePublisher.accept(ex.getMessage());
    }

    private Member savePoint(String memberId, String memberName, long point) {
        Member member = loadMember(memberId, memberName);
        member.savePoint(point);
        saveMemberPort.saveMember(member);
        return member;
    }

    private Member usePoint(String memberId, String memberName, long point) {
        Member member = loadMember(memberId, memberName);
        member.usePoint(point);
        saveMemberPort.saveMember(member);
        return member;
    }

    private <T extends MemberDomainEvent> T pullRequiredEvent(Member member, Class<T> eventType) {
        return member.pullDomainEvents()
            .stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Expected domain event was not raised: " + eventType.getSimpleName()
            ));
    }

    private Member loadMember(String memberId, String memberName) {
        return loadMemberByIdNamePort.loadMemberByIdName(new MemberIdentity(memberId, memberName))
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + memberId));
    }

}
