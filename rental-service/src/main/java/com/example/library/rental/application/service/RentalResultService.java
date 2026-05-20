package com.example.library.rental.application.service;

import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
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
import com.example.library.rental.domain.event.RentalDomainEvent;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
import com.example.library.rental.domain.model.saga.RentalSagaParticipant;
import com.example.library.rental.domain.model.saga.RentalSagaState;
import com.example.library.rental.domain.model.saga.RentalSagaType;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * book/member 결과 이벤트를 {@code correlationId} 기준 SAGA 상태에 반영하고 실패 시 보상하는 application service.
 *
 * <p>{@link MessageIdempotencyPort}로 결과 이벤트 {@code eventId} 중복을 막고,
 * Kafka 메시지 생성과 Avro 변환은 outbound messaging adapter에 맡깁니다.
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
     * 참여 서비스의 결과 이벤트를 처리합니다.
     *
     * <p>{@link MessageIdempotencyPort}에 {@code eventId}를 먼저 기록해 재전달된 결과 이벤트를 건너뜁니다.
     * 새 이벤트이면 {@code correlationId}로 {@link RentalSagaState}를 찾고, 없으면
     * {@link #fallbackState(RentalResultCommand)}로 최소 상태를 복원합니다. 참여자 결과 저장 후 실패가 있으면
     * {@link #compensate(RentalSagaState)}를 실행합니다.
     *
     * @param command 처리할 참여 서비스 결과 application command.
     */
    @Override
    @Transactional
    public void handle(RentalResultCommand command) {
        processIfNew(command, () -> {
            RentalSagaState state = loadOrFallback(command);
            recordParticipantResult(state, command);
            saveRentalSagaStatePort.save(state);

            if (!state.hasFailure()) {
                logSuccess(command);
                return;
            }

            compensate(state);
            saveRentalSagaStatePort.save(state);
        });
    }

    /**
     * 아직 처리하지 않은 결과 이벤트일 때만 후속 handler를 실행합니다.
     *
     * @param command 멱등성 판단에 사용할 결과 이벤트 command.
     * @param handler 새 이벤트일 때 실행할 SAGA 처리 흐름.
     */
    private void processIfNew(RentalResultCommand command, Runnable handler) {
        if (!messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.EVENT_RESULT
        )) {
            log.info("skip already processed rental_result eventId={}", command.eventId());
            return;
        }
        handler.run();
    }

    /**
     * Result Event의 참여자 성공/실패를 SAGA 추적 상태에 반영합니다.
     *
     * @param state 결과를 기록할 SAGA 상태.
     * @param command 참여자 결과가 담긴 application command.
     */
    private void recordParticipantResult(RentalSagaState state, RentalResultCommand command) {
        state.recordParticipantResult(
            command.sourceEventId(),
            toSagaParticipant(command.participant()),
            command.successed()
        );
    }

    /**
     * {@code correlationId}로 SAGA 상태를 찾고, 없으면 결과 이벤트 snapshot으로 최소 상태를 만듭니다.
     *
     * @param command SAGA 상태 조회와 fallback 생성에 사용할 결과 command.
     * @return 저장된 SAGA 상태 또는 결과 이벤트에서 복원한 최소 SAGA 상태.
     */
    private RentalSagaState loadOrFallback(RentalResultCommand command) {
        return loadRentalSagaStatePort.loadByCorrelationId(command.correlationId())
            .orElseGet(() -> fallbackState(command));
    }

    /**
     * 성공한 참여자 결과를 운영 로그로 남깁니다.
     *
     * @param command 로그에 남길 결과 이벤트 command.
     */
    private void logSuccess(RentalResultCommand command) {
        log.info(
            "participant success eventType={} participant={} step={} eventId={}",
            command.eventType(),
            command.participant(),
            command.step(),
            command.eventId()
        );
    }

    /**
     * 저장된 SAGA 상태가 없을 때 결과 이벤트 snapshot으로 최소 상태를 만듭니다.
     *
     * <p>정상 흐름에서는 대여/반납/연체 해제 요청 시 {@link RentalSagaState}가 먼저 저장됩니다.
     * 이 메서드는 운영 재처리나 이전 메시지도 보상 판단까지 이어가기 위한 방어 코드입니다.
     *
     * @param command SAGA 상태 복원에 사용할 참여 서비스 결과 command.
     * @return 결과 이벤트 내용으로 복원한 최소 SAGA 상태.
     */
    private RentalSagaState fallbackState(RentalResultCommand command) {
        RentalMember member = new RentalMember(command.memberId(), command.memberName());
        RentalItem item = command.itemNo() == null ? null : new RentalItem(command.itemNo(), command.itemTitle());
        return switch (toSagaType(command.eventType())) {
            case RENT -> RentalSagaState.startRent(command.correlationId(), member, item, command.point());
            case RETURN -> RentalSagaState.startReturn(command.correlationId(), member, item, command.point());
            case OVERDUE -> RentalSagaState.startOverdue(command.correlationId(), member, command.point());
        };
    }

    /**
     * 실패한 SAGA 유형에 맞춰 rental 상태와 성공한 참여자만 되돌립니다.
     *
     * @param state 실패가 기록된 SAGA 상태.
     */
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

    /**
     * 공유 결과 이벤트의 {@link EventType}을 rental-service 내부 SAGA 유형으로 변환합니다.
     *
     * @param eventType 결과 이벤트에 담긴 공유 이벤트 유형.
     * @return rental-service 내부 SAGA 유형.
     */
    private RentalSagaType toSagaType(EventType eventType) {
        return switch (eventType) {
            case RENT -> RentalSagaType.RENT;
            case RETURN -> RentalSagaType.RETURN;
            case OVERDUE -> RentalSagaType.OVERDUE;
        };
    }

    /**
     * 공유 결과 이벤트의 {@link Participant}를 rental-service 내부 참여자 enum으로 변환합니다.
     *
     * @param participant 결과 이벤트를 발행한 참여 서비스.
     * @return rental-service 내부 참여자 enum.
     */
    private RentalSagaParticipant toSagaParticipant(Participant participant) {
        return switch (participant) {
            case BOOK -> RentalSagaParticipant.BOOK;
            case MEMBER -> RentalSagaParticipant.MEMBER;
        };
    }

    /**
     * RENT 실패 시 대여 상태를 되돌리고 대여 취소 이벤트를 발행합니다.
     *
     * <p>{@link CompensationIdempotencyPort}로 같은 {@code correlationId}의 RENT_CANCEL 중복 실행을 막습니다.
     * 이벤트는 {@link RentalCard#cancelRentItem(RentalItem)}이 기록한 {@link ItemRentCanceledDomainEvent}를 사용합니다.
     *
     * @param member 대여를 취소할 회원 snapshot.
     * @param item 대여 취소 대상 도서 snapshot.
     * @param correlationId 원래 RENT 흐름의 상관관계 ID.
     */
    private void cancelRentItem(RentalMember member, RentalItem item, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelRentItem(item);
        saveRentalCardPort.save(rentalCard);
        pullEvent(rentalCard, ItemRentCanceledDomainEvent.class)
            .ifPresent(event -> publishItemRentCanceledPort.publishRentCanceledEvent(event, correlationId));
    }

    /**
     * RENT 실패 시 member-service에 이미 적립된 대여 포인트 차감을 요청합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot.
     * @param correlationId 원래 RENT 흐름의 상관관계 ID.
     */
    private void compensateRentPoint(RentalMember member, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishRentPointUseCommand(
            member,
            RentalPointPolicy.RENT.point(),
            correlationId
        );
    }

    /**
     * RETURN 실패 시 반납 상태를 되돌리고 반납 취소 이벤트를 발행합니다.
     *
     * <p>이벤트는 {@link RentalCard#cancelReturnItem(RentalItem, long)}이 기록한
     * {@link ItemReturnCanceledDomainEvent}를 사용합니다.
     *
     * @param member 반납을 취소할 회원 snapshot.
     * @param item 반납 취소 대상 도서 snapshot.
     * @param point 반납 흐름에서 되돌려야 할 포인트.
     * @param correlationId 원래 RETURN 흐름의 상관관계 ID.
     */
    private void cancelReturnItem(RentalMember member, RentalItem item, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelReturnItem(item, point);
        saveRentalCardPort.save(rentalCard);
        pullEvent(rentalCard, ItemReturnCanceledDomainEvent.class)
            .ifPresent(event -> publishItemReturnCanceledPort.publishReturnCanceledEvent(event, correlationId));
    }

    /**
     * RETURN 실패 시 member-service에 이미 적립된 반납 포인트 차감을 요청합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot.
     * @param point 차감할 반납 포인트.
     * @param correlationId 원래 RETURN 흐름의 상관관계 ID.
     */
    private void compensateReturnPoint(RentalMember member, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishReturnPointUseCommand(member, point, correlationId);
    }

    /**
     * OVERDUE 실패 시 연체 해제 상태를 되돌리고 취소 이벤트를 발행합니다.
     *
     * <p>이벤트는 {@link RentalCard#cancelMakeAvailableRental(long)}이 기록한
     * {@link OverdueClearCanceledDomainEvent}를 사용합니다.
     *
     * @param member 연체 해제를 취소할 회원 snapshot.
     * @param point 다시 부과할 연체 포인트.
     * @param correlationId 원래 OVERDUE 흐름의 상관관계 ID.
     */
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

        pullEvent(rentalCard, OverdueClearCanceledDomainEvent.class)
            .ifPresent(event -> publishOverdueClearCanceledPort.publishOverdueClearCanceledEvent(
                event,
                correlationId
            ));
    }

    /**
     * 보상 상태 변경 중 aggregate가 실제로 기록한 이벤트만 꺼냅니다.
     *
     * <p>{@code pullDomainEvents()}는 반환 후 내부 이벤트 버퍼를 비우므로, 하나의 보상 상태 변경에 한 번만 호출합니다.
     *
     * @param rentalCard 보상 상태 변경을 수행한 대여카드 aggregate.
     * @param eventType 발행 대상으로 찾을 도메인 이벤트 타입.
     * @return aggregate가 실제로 기록한 보상 도메인 이벤트.
     */
    private <T extends RentalDomainEvent> Optional<T> pullEvent(RentalCard rentalCard, Class<T> eventType) {
        return rentalCard.pullDomainEvents()
            .stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .findFirst();
    }

    /**
     * 보상 대상 대여카드를 조회합니다.
     *
     * <p>보상은 기존 상태를 되돌리는 흐름이므로 대여카드가 없으면 상태 불일치로 보고 예외를 발생시킵니다.
     *
     * @param idName 대여카드 소유 회원 snapshot.
     * @return 보상 대상 대여카드 aggregate.
     */
    private RentalCard load(RentalMember idName) {
        return loadRentalCardPort.loadRentalCard(idName.id())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

}
