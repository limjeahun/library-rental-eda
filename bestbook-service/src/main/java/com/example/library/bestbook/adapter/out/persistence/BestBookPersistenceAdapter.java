package com.example.library.bestbook.adapter.out.persistence;

import com.example.library.bestbook.adapter.out.persistence.mapper.BestBookPersistenceMapper;
import com.example.library.bestbook.adapter.out.persistence.repository.BestBookJpaRepository;
import com.example.library.bestbook.application.port.out.BestBookPort;
import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BestBookPersistenceAdapter implements BestBookPort {
    private final BestBookJpaRepository repository;
    private final BestBookPersistenceMapper mapper;

    public BestBookPersistenceAdapter(BestBookJpaRepository repository, BestBookPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<BestBook> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<BestBook> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BestBook> findByItemNo(Long itemNo) {
        return repository.findByItemNo(itemNo).map(mapper::toDomain);
    }

    @Override
    public BestBook save(BestBook bestBook) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(bestBook)));
    }
}
