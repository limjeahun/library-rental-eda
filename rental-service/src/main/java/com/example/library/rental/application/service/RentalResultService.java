package com.example.library.rental.application.service;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import com.example.library.rental.application.dto.RentalSagaState;
import com.example.library.rental.application.port.out.LoadRentalSagaStatePort;
import com.example.library.rental.application.port.out.MessageIdempotencyPort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서/회원 서비스의 성공/실패 결과 이벤트를 해석해 대여, 반납, 연체 해제 보상을 실행하는 application service입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RentalResultService implements HandleRentalResultUseCase {
    private static final String SERVICE_NAME = "rental-service";

    private final CompensationUseCase compensationUseCase;
    private final MessageIdempotencyPort messageIdempotencyPort;
    private final LoadRentalSagaStatePort loadRentalSagaStatePort;
    private final SaveRentalSagaStatePort saveRentalSagaStatePort;

    /**
     * 성공 결과는 기록만 하고, 실패 결과는 이벤트 타입별 보상 흐름으로 분기합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    @Override
    @Transactional
    public void handle(EventResult result) {
        if (!messageIdempotencyPort.markProcessed(SERVICE_NAME, result.eventId(), result.correlationId(), "EventResult")) {
            log.info("skip already processed rental_result eventId={}", result.eventId());
            return;
        }

        RentalSagaState state = loadRentalSagaStatePort.loadByCorrelationId(result.correlationId())
            .orElseGet(() -> fallbackState(result));
        state.recordResult(result);
        saveRentalSagaStatePort.save(state);

        if (!state.hasFailure()) {
            log.info(
                "participant success eventType={} participant={} step={} eventId={}",
                result.eventType(),
                result.participant(),
                result.step(),
                result.eventId()
            );
            return;
        }

        compensate(state);
        saveRentalSagaStatePort.save(state);
    }

    private RentalSagaState fallbackState(EventResult result) {
        RentalMember member = new RentalMember(result.memberId(), result.memberName());
        RentalItem item = result.itemNo() == null ? null : new RentalItem(result.itemNo(), result.itemTitle());
        if (result.eventType() == EventType.RENT) {
            return RentalSagaState.startRent(result.correlationId(), member, item, result.point());
        }
        if (result.eventType() == EventType.RETURN) {
            return RentalSagaState.startReturn(result.correlationId(), member, item, result.point());
        }
        if (result.eventType() == EventType.OVERDUE) {
            return RentalSagaState.startOverdue(result.correlationId(), member, result.point());
        }
        throw new IllegalArgumentException("unsupported eventType=" + result.eventType());
    }

    private void compensate(RentalSagaState state) {
        if (state.eventType() == EventType.RENT) {
            compensationUseCase.cancelRentItem(state.idName(), state.item(), state.correlationId());
            if (state.isMemberSuccess()) {
                compensationUseCase.compensateRentPoint(state.idName(), state.correlationId());
            }
            return;
        }
        if (state.eventType() == EventType.RETURN) {
            compensationUseCase.cancelReturnItem(state.idName(), state.item(), state.point(), state.correlationId());
            if (state.isMemberSuccess()) {
                compensationUseCase.compensateReturnPoint(state.idName(), state.point(), state.correlationId());
            }
            return;
        }
        if (state.eventType() == EventType.OVERDUE) {
            compensationUseCase.cancelMakeAvailableRental(state.idName(), state.point(), state.correlationId());
        }
    }
}
