package com.example.library.rental.application.service;

import com.example.library.common.event.EventResult;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도서/회원 서비스의 성공/실패 결과 이벤트를 해석해 대여, 반납, 연체 해제 보상을 실행하는 application service입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RentalResultService implements HandleRentalResultUseCase {
    private final CompensationUseCase compensationUseCase;

    /**
     * 성공 결과는 기록만 하고, 실패 결과는 이벤트 타입별 보상 흐름으로 분기합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    @Override
    public void handle(EventResult result) {
        if (result.successed()) {
            log.info("participant success eventType={} eventId={}", result.eventType(), result.eventId());
            return;
        }

        switch (result.eventType()) {
            case RENT -> compensationUseCase.cancelRentItem(result.idName(), result.item(), result.correlationId());
            case RETURN -> compensationUseCase.cancelReturnItem(result.idName(), result.item(), result.point(), result.correlationId());
            case OVERDUE -> compensationUseCase.cancelMakeAvailableRental(result.idName(), result.point(), result.correlationId());
            default -> log.warn("unsupported eventType={}", result.eventType());
        }
    }
}
