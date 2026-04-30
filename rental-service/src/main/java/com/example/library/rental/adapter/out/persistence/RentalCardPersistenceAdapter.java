package com.example.library.rental.adapter.out.persistence;

import com.example.library.rental.adapter.out.persistence.mapper.RentalCardPersistenceMapper;
import com.example.library.rental.adapter.out.persistence.repository.RentalCardJpaRepository;
import com.example.library.rental.application.port.out.RentalCardOutputPort;
import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RentalCardPersistenceAdapter implements RentalCardOutputPort {
    private final RentalCardJpaRepository repository;
    private final RentalCardPersistenceMapper mapper;

    public RentalCardPersistenceAdapter(RentalCardJpaRepository repository, RentalCardPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RentalCard save(RentalCard rentalCard) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(rentalCard)));
    }

    @Override
    public Optional<RentalCard> loadRentalCard(String userId) {
        return repository.findByMemberId(userId).map(mapper::toDomain);
    }
}
