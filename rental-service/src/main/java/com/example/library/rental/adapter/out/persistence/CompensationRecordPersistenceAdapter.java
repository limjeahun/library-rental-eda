package com.example.library.rental.adapter.out.persistence;

import com.example.library.rental.adapter.out.persistence.repository.CompensationRecordJpaRepository;
import com.example.library.rental.application.port.out.CompensationIdempotencyPort;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * correlationId와 보상 타입 기준의 보상 실행 이력을 저장하는 outbound persistence adapter입니다.
 *
 * <p>메시지 중복과 별도로 같은 비즈니스 보상이 두 번 실행되지 않게 합니다.
 */
@Repository
@RequiredArgsConstructor
public class CompensationRecordPersistenceAdapter implements CompensationIdempotencyPort {
    private final CompensationRecordJpaRepository repository;

    @Override
    @Transactional
    public boolean markCompensated(String correlationId, RentalCompensationType compensationType) {
        validate(correlationId, compensationType);
        return repository.insertIgnore(correlationId, compensationType.name()) == 1;
    }

    private void validate(String correlationId, RentalCompensationType compensationType) {
        // 두 값이 보상 멱등성 키입니다.
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId는 비어 있을 수 없습니다.");
        }
        if (compensationType == null) {
            throw new IllegalArgumentException("compensationType은 null일 수 없습니다.");
        }
    }
}
