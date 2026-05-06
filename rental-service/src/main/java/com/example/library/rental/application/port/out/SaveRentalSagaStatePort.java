package com.example.library.rental.application.port.out;

import com.example.library.rental.application.dto.RentalSagaState;

/**
 * correlationId 기준 SAGA 추적 상태를 저장하는 outbound port입니다.
 */
public interface SaveRentalSagaStatePort {
    RentalSagaState save(RentalSagaState state);
}
