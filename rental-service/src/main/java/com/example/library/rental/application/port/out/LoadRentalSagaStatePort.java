package com.example.library.rental.application.port.out;

import com.example.library.rental.application.dto.RentalSagaState;
import java.util.Optional;

/**
 * correlationId 기준 SAGA 추적 상태를 조회하는 outbound port입니다.
 */
public interface LoadRentalSagaStatePort {
    Optional<RentalSagaState> loadByCorrelationId(String correlationId);
}
