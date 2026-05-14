package com.example.library.rental.application.service;

import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.PointUseReason;
import com.example.library.rental.application.dto.PointUseCommandPayload;
import com.example.library.rental.application.dto.RentalResultCommand;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import com.example.library.rental.application.port.out.CompensationIdempotencyPort;
import com.example.library.rental.application.port.out.LoadRentalCardPort;
import com.example.library.rental.application.port.out.LoadRentalSagaStatePort;
import com.example.library.rental.application.port.out.MessageIdempotencyPort;
import com.example.library.rental.application.port.out.PublishItemRentCanceledPort;
import com.example.library.rental.application.port.out.PublishItemReturnCanceledPort;
import com.example.library.rental.application.port.out.PublishOverdueClearCanceledPort;
import com.example.library.rental.application.port.out.PublishPointUseCommandPort;
import com.example.library.rental.application.port.out.SaveRentalCardPort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;
import com.example.library.rental.domain.event.OverdueClearCanceledDomainEvent;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
import com.example.library.rental.domain.model.saga.RentalSagaParticipant;
import com.example.library.rental.domain.model.saga.RentalSagaState;
import com.example.library.rental.domain.model.saga.RentalSagaType;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서/회원 서비스의 성공/실패 결과 이벤트를 해석해 대여, 반납, 연체 해제 보상을 실행하는 application service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RentalResultService implements HandleRentalResultUseCase {
    private final MessageIdempotencyPort          messageIdempotencyPort;
    private final LoadRentalSagaStatePort         loadRentalSagaStatePort;
    private final SaveRentalSagaStatePort         saveRentalSagaStatePort;
    private final LoadRentalCardPort              loadRentalCardPort;
    private final SaveRentalCardPort              saveRentalCardPort;
    private final PublishPointUseCommandPort      publishPointUseCommandPort;
    private final PublishItemRentCanceledPort     publishItemRentCanceledPort;
    private final PublishItemReturnCanceledPort   publishItemReturnCanceledPort;
    private final PublishOverdueClearCanceledPort publishOverdueClearCanceledPort;
    private final CompensationIdempotencyPort     compensationIdempotencyPort;

    /**
     * 성공 결과는 기록만 하고, 실패 결과는 이벤트 타입별 보상 흐름으로 분기.
     *
     * @param command 처리할 참여 서비스 결과 application command.
     */
    @Override
    @Transactional
    public void handle(RentalResultCommand command) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.EVENT_RESULT
        )) {
            log.info("skip already processed rental_result eventId={}", command.eventId());
            return;
        }

        RentalSagaState state = loadRentalSagaStatePort.loadByCorrelationId(command.correlationId())
            .orElseGet(() -> fallbackState(command));
        state.recordParticipantResult(
            command.sourceEventId(),
            toSagaParticipant(command.participant()),
            command.successed()
        );
        saveRentalSagaStatePort.save(state);

        if (!state.hasFailure()) {
            log.info(
                "participant success eventType={} participant={} step={} eventId={}",
                command.eventType(),
                command.participant(),
                command.step(),
                command.eventId()
            );
            return;
        }

        compensate(state);
        saveRentalSagaStatePort.save(state);
    }

    private RentalSagaState fallbackState(RentalResultCommand command) {
        RentalMember member = new RentalMember(command.memberId(), command.memberName());
        RentalItem item = command.itemNo() == null ? null : new RentalItem(command.itemNo(), command.itemTitle());
        return switch (toSagaType(command.eventType())) {
            case RENT -> RentalSagaState.startRent(command.correlationId(), member, item, command.point());
            case RETURN -> RentalSagaState.startReturn(command.correlationId(), member, item, command.point());
            case OVERDUE -> RentalSagaState.startOverdue(command.correlationId(), member, command.point());
        };
    }

    private void compensate(RentalSagaState state) {
        switch (state.sagaType()) {
            case RENT -> {
                cancelRentItem(state.member(), state.item(), state.correlationId());
                if (state.isMemberSuccess()) {
                    compensateRentPoint(state.member(), state.correlationId());
                }
            }
            case RETURN -> {
                cancelReturnItem(state.member(), state.item(), state.point(), state.correlationId());
                if (state.isMemberSuccess()) {
                    compensateReturnPoint(state.member(), state.point(), state.correlationId());
                }
            }
            case OVERDUE -> cancelMakeAvailableRental(
                state.member(),
                state.point(),
                state.correlationId()
            );
        }
    }

    private RentalSagaType toSagaType(EventType eventType) {
        return switch (eventType) {
            case RENT -> RentalSagaType.RENT;
            case RETURN -> RentalSagaType.RETURN;
            case OVERDUE -> RentalSagaType.OVERDUE;
        };
    }

    private RentalSagaParticipant toSagaParticipant(Participant participant) {
        return switch (participant) {
            case BOOK -> RentalSagaParticipant.BOOK;
            case MEMBER -> RentalSagaParticipant.MEMBER;
        };
    }

    private void cancelRentItem(RentalMember member, RentalItem item, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelRentItem(item);
        saveRentalCardPort.save(rentalCard);
        publishItemRentCanceledPort.publishRentCanceledEvent(
            new ItemRentCanceledDomainEvent(member, item, RentalPointPolicy.RENT.point()),
            correlationId
        );
    }

    private void compensateRentPoint(RentalMember member, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishPointUseCommand(
            createPointUseCommand(
                correlationId,
                member,
                RentalPointPolicy.RENT.point(),
                PointUseReason.RENT_COMPENSATION
            )
        );
    }

    private void cancelReturnItem(RentalMember member, RentalItem item, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelReturnItem(item, point);
        saveRentalCardPort.save(rentalCard);
        publishItemReturnCanceledPort.publishReturnCanceledEvent(
            new ItemReturnCanceledDomainEvent(member, item, point),
            correlationId
        );
    }

    private void compensateReturnPoint(RentalMember member, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishPointUseCommand(
            createPointUseCommand(correlationId, member, point, PointUseReason.RETURN_COMPENSATION)
        );
    }

    private void cancelMakeAvailableRental(RentalMember member, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(
            correlationId,
            RentalCompensationType.OVERDUE_CLEAR_CANCEL
        )) {
            return;
        }

        RentalCard rentalCard = load(member);
        rentalCard.cancelMakeAvailableRental(point);
        saveRentalCardPort.save(rentalCard);

        publishOverdueClearCanceledPort.publishOverdueClearCanceledEvent(
            new OverdueClearCanceledDomainEvent(member, point),
            correlationId
        );
    }

    private RentalCard load(RentalMember idName) {
        return loadRentalCardPort.loadRentalCard(idName.id())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    private PointUseCommandPayload createPointUseCommand(
        String correlationId,
        RentalMember member,
        long point,
        PointUseReason reason
    ) {
        String commandCorrelationId = correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId;
        return new PointUseCommandPayload(commandCorrelationId, member.id(), member.name(), point, reason);
    }
}
