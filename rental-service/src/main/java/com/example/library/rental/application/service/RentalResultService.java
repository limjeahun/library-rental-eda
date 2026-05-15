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
 * 도서/회원 서비스의 처리 결과 이벤트를 해석해 rental-service 의 SAGA 상태와 보상 흐름을 조율하는 application service.
 *
 * <p>이 서비스가 받는 입력은 rental-service 가 처음 발행한 대여/반납/연체 해제 이벤트가 아니라,
 * book-service 또는 member-service 가 해당 이벤트를 처리한 뒤 발행한 결과 이벤트입니다.
 *
 * <p>주요 책임은 다음과 같습니다.
 * <p>1. 결과 이벤트 자체가 이미 처리됐는지 확인합니다.
 * <p>2. {@code correlationId} 기준으로 SAGA 상태를 조회하고 참여자 결과를 기록합니다.
 * <p>3. 모든 참여자가 성공 중이면 보상 없이 종료합니다.
 * <p>4. 실패가 확인되면 대여/반납/연체 해제 흐름에 맞는 보상을 한 번만 실행합니다.
 *
 * <p>Kafka 메시지 생성, 토픽 선택, Avro 변환은 outbound messaging adapter 의 책임입니다.
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
     * <p>동일한 결과 이벤트가 재전달될 수 있으므로 먼저 {@link MessageIdempotencyPort} 로
     * 결과 이벤트의 {@code eventId} 를 처리 완료로 기록합니다. 이미 처리된 결과 이벤트라면
     * SAGA 상태나 대여카드 상태를 다시 변경하지 않고 종료합니다.
     *
     * <p>처리 가능한 결과 이벤트는 다음과 같은 의미를 가집니다.
     * <p>- book-service 결과: 도서 상태 변경 성공/실패
     * <p>- member-service 결과: 포인트 적립/차감 성공/실패
     *
     * <p>정상 경로에서는 rental-service 가 대여/반납/연체 해제 요청을 시작할 때 저장한
     * {@link RentalSagaState} 를 {@code correlationId} 로 찾습니다. 상태가 없으면 늦게 도착한
     * 결과 이벤트도 최소한 해석할 수 있도록 {@link #fallbackState(RentalResultCommand)} 로
     * 보상에 필요한 최소 SAGA 상태를 복원합니다.
     *
     * <p>참여자 결과를 기록한 뒤 아직 실패가 없다면 결과 기록만 하고 종료합니다.
     * 하나라도 실패가 확인되면 {@link #compensate(RentalSagaState)} 로 현재 SAGA 유형에 맞는
     * 보상 흐름을 실행합니다.
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

    /**
     * 저장된 SAGA 상태를 찾지 못했을 때 결과 이벤트 내용으로 최소 상태를 복원합니다.
     *
     * <p>정상 흐름에서는 {@code rentItem}, {@code returnItem}, {@code clearOverdue} 가 먼저
     * {@link RentalSagaState} 를 저장한 뒤 외부 이벤트를 발행합니다. 하지만 로컬 데이터 초기화,
     * 이전 버전 메시지, 운영 중 수동 재처리 같은 상황에서는 결과 이벤트가 도착했는데 상태가
     * 없을 수 있습니다.
     *
     * <p>이 메서드는 그런 예외 상황에서 보상 판단에 필요한 회원, 도서, 포인트, SAGA 유형만
     * 결과 command 로부터 복원합니다. 완전한 정상 상태를 보장하기 위한 메서드가 아니라,
     * 결과 이벤트를 버리지 않고 최소한의 보상 흐름으로 연결하기 위한 방어 코드입니다.
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
     * SAGA 유형별 보상 흐름을 실행합니다.
     *
     * <p>현재 서비스는 중앙 오케스트레이터를 두지 않고, rental-service 가 자신이 시작한 흐름의
     * 결과를 추적하다가 실패를 발견하면 자기 상태와 이미 성공한 참여자 상태만 되돌립니다.
     *
     * <p>RENT 보상:
     * 대여카드에서 대여 항목을 제거하고, member-service 포인트 적립이 성공한 상태라면
     * 대여 포인트 차감 command 를 발행합니다.
     *
     * <p>RETURN 보상:
     * 반납 완료 항목을 다시 대여 중 목록으로 되돌리고, member-service 반납 포인트 적립이
     * 성공한 상태라면 반납 포인트 차감 command 를 발행합니다.
     *
     * <p>OVERDUE 보상:
     * 연체 해제로 풀린 대여 가능 상태를 다시 연체/대여 불가 상태로 되돌립니다.
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
     * 공유 결과 이벤트의 {@link EventType} 을 rental-service 내부 SAGA 유형으로 변환합니다.
     *
     * <p>{@link EventType} 은 서비스 간 메시지 계약이고, {@link RentalSagaType} 은 rental-service
     * 내부 상태 모델입니다. application service 는 외부 계약을 그대로 domain 상태 모델에 흘리지 않고
     * 이 메서드에서 내부 타입으로 바꿔 사용합니다.
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
     * 공유 결과 이벤트의 참여자 값을 rental-service 내부 참여자 상태 값으로 변환합니다.
     *
     * <p>{@link Participant} 는 common-events 의 메시지 계약이고,
     * {@link RentalSagaParticipant} 는 {@link RentalSagaState} 가 참여자별 성공/실패를 기록하기 위해
     * 사용하는 내부 domain enum 입니다.
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
     * RENT 흐름 실패 시 rental-service 의 대여 상태를 취소하고 대여 취소 이벤트를 발행합니다.
     *
     * <p>예를 들어 대여카드에는 도서가 대여 중으로 저장됐지만 book-service 의 도서 상태 변경이나
     * member-service 의 포인트 적립 중 하나가 실패하면, rental-service 는 자신이 만든 대여 상태를
     * 되돌려야 합니다.
     *
     * <p>{@link CompensationIdempotencyPort} 는 같은 {@code correlationId} 에 대해 대여 취소 보상이
     * 여러 번 실행되는 것을 막습니다. 보상을 처음 실행하는 경우에만 대여카드를 저장합니다.
     *
     * <p>대여 취소 이벤트는 application service 가 직접 만들지 않습니다. {@link RentalCard} 가
     * 실제 대여 취소 상태 변경을 수행하면서 {@link ItemRentCanceledDomainEvent} 를 내부에 기록하고,
     * application service 는 저장 이후 그 이벤트를 꺼내 outbound port 로 전달합니다.
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
     * RENT 흐름 실패 시 member-service 에 이미 적립된 대여 포인트 차감을 요청합니다.
     *
     * <p>이 보상은 member-service 의 포인트 적립 결과가 성공으로 확인된 경우에만 호출됩니다.
     * 포인트 적립이 실패했거나 아직 성공이 확인되지 않은 상태라면 차감 command 를 보내면 안 됩니다.
     *
     * <p>실제 Kafka command 인 {@code PointUseCommand} 생성, {@code PointUseReason} 선택,
     * {@code eventId} 생성은 outbound messaging adapter 가 담당합니다. 이 메서드는
     * "대여 포인트 보상 차감이 필요하다"는 application 의 의도만 port 로 전달합니다.
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
     * RETURN 흐름 실패 시 rental-service 의 반납 상태를 취소하고 반납 취소 이벤트를 발행합니다.
     *
     * <p>반납 흐름에서는 rental-service 가 대여 중 항목을 반납 완료 목록으로 옮긴 뒤,
     * book-service 에 도서 상태를 대여 가능으로 바꾸도록 이벤트를 발행합니다. 이후 참여 서비스 중
     * 하나가 실패하면 반납 완료 처리를 되돌려 도서를 다시 대여 중 목록으로 복원해야 합니다.
     *
     * <p>보상이 처음 실행되는 경우에만 대여카드를 저장합니다. 반납 취소 이벤트는
     * {@link RentalCard#cancelReturnItem(RentalItem, long)} 이 실제 상태를 되돌릴 때
     * aggregate 내부에 기록하고, application service 는 저장 이후 해당 이벤트를 꺼내 발행합니다.
     *
     * @param member 반납을 취소할 회원 snapshot.
     * @param item 반납 취소 대상 도서 snapshot.
     * @param point 반납 흐름에서 적립됐거나 되돌려야 할 포인트.
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
     * RETURN 흐름 실패 시 member-service 에 이미 적립된 반납 포인트 차감을 요청합니다.
     *
     * <p>이 보상은 member-service 의 반납 포인트 적립 결과가 성공으로 확인된 경우에만 호출됩니다.
     * 실제 command 메시지 조립은 outbound messaging adapter 에 맡기고, application service 는
     * 어떤 회원의 어떤 포인트를 어떤 흐름에서 되돌릴지만 명시합니다.
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
     * OVERDUE 흐름 실패 시 rental-service 의 연체 해제 상태를 취소합니다.
     *
     * <p>연체 해제 흐름에서는 회원이 포인트를 사용해 연체료를 정산하고 대여 가능 상태로 돌아갑니다.
     * 이후 member-service 의 포인트 차감 등 참여 결과가 실패하면 rental-service 는 다시 연체료와
     * 대여 불가 상태를 복원해야 합니다.
     *
     * <p>보상이 처음 실행되는 경우에만 대여카드를 저장합니다. 연체 해제 취소 이벤트는
     * {@link RentalCard#cancelMakeAvailableRental(long)} 이 실제 연체 상태를 복원할 때
     * aggregate 내부에 기록하고, application service 는 저장 이후 해당 이벤트를 꺼내 발행합니다.
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
     * 보상 상태 변경 중 aggregate 가 기록한 도메인 이벤트를 꺼냅니다.
     *
     * <p>보상 메서드는 멱등하게 동작하므로 이미 보상된 상태에서는 aggregate 가 아무 상태도 바꾸지
     * 않을 수 있습니다. 이 경우 발행할 도메인 이벤트도 없으므로 {@link Optional#empty()} 를 반환합니다.
     *
     * <p>{@code pullDomainEvents()} 는 반환 후 내부 이벤트 버퍼를 비우므로, 하나의 보상 상태 변경에
     * 대해 한 번만 호출해야 합니다.
     *
     * @param rentalCard 보상 상태 변경을 수행한 대여카드 aggregate.
     * @param eventType 발행 대상으로 찾을 도메인 이벤트 타입.
     * @return aggregate 가 실제로 기록한 보상 도메인 이벤트.
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
     * <p>결과 이벤트 보상은 기존 대여카드 상태를 되돌리는 흐름이므로 대여카드가 없으면
     * 새로 만들지 않습니다. 보상할 aggregate 가 없다는 것은 결과 이벤트와 rental-service 상태가
     * 어긋난 것이므로 예외를 발생시켜 재처리 또는 운영 확인 대상으로 남깁니다.
     *
     * @param idName 대여카드 소유 회원 snapshot.
     * @return 보상 대상 대여카드 aggregate.
     */
    private RentalCard load(RentalMember idName) {
        return loadRentalCardPort.loadRentalCard(idName.id())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

}
