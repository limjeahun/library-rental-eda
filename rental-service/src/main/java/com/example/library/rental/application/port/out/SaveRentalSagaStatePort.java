package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.saga.RentalSagaState;

/**
 * correlationId 기준 SAGA 추적 상태를 저장하는 outbound port.
 */
public interface SaveRentalSagaStatePort {
    RentalSagaState save(RentalSagaState state);
}
