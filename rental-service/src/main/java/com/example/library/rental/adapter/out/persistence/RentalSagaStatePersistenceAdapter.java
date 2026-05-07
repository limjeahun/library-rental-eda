package com.example.library.rental.adapter.out.persistence;

import com.example.library.rental.adapter.out.persistence.entity.RentalSagaStateJpaEntity;
import com.example.library.rental.adapter.out.persistence.repository.RentalSagaStateJpaRepository;
import com.example.library.rental.application.port.out.LoadRentalSagaStatePort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.model.RentalSagaState;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * rental-service SAGA 추적 상태를 JPA 엔티티로 저장하고 domain model로 복원하는 adapter입니다.
 */
@Repository
@RequiredArgsConstructor
public class RentalSagaStatePersistenceAdapter implements LoadRentalSagaStatePort, SaveRentalSagaStatePort {
    private final RentalSagaStateJpaRepository repository;

    @Override
    public Optional<RentalSagaState> loadByCorrelationId(String correlationId) {
        return repository.findById(correlationId).map(this::toState);
    }

    @Override
    public RentalSagaState save(RentalSagaState state) {
        return toState(repository.save(toEntity(state)));
    }

    private RentalSagaStateJpaEntity toEntity(RentalSagaState state) {
        RentalItem item = state.item();
        return new RentalSagaStateJpaEntity(
            state.correlationId(),
            state.sourceEventId(),
            state.sagaType(),
            state.idName().id(),
            state.idName().name(),
            item == null ? null : item.no(),
            item == null ? null : item.title(),
            state.point(),
            state.bookResult(),
            state.memberResult(),
            state.sagaStatus(),
            state.startedAt(),
            state.updatedAt()
        );
    }

    private RentalSagaState toState(RentalSagaStateJpaEntity entity) {
        RentalItem item = entity.getItemNo() == null ? null : new RentalItem(entity.getItemNo(), entity.getItemTitle());
        return RentalSagaState.reconstitute(
            entity.getCorrelationId(),
            entity.getSourceEventId(),
            entity.getSagaType(),
            new RentalMember(entity.getMemberId(), entity.getMemberName()),
            item,
            entity.getPoint(),
            entity.getBookResult(),
            entity.getMemberResult(),
            entity.getSagaStatus(),
            entity.getStartedAt(),
            entity.getUpdatedAt()
        );
    }
}
