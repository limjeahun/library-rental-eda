package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCompensationType;

/**
 * 같은 비즈니스 보상이 여러 실패 결과로 중복 실행되지 않게 기록하는 outbound port입니다.
 */
public interface CompensationIdempotencyPort {
    boolean markCompensated(String correlationId, RentalCompensationType compensationType);
}
