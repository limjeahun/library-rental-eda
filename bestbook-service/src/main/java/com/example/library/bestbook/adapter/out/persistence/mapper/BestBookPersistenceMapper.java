package com.example.library.bestbook.adapter.out.persistence.mapper;

import com.example.library.bestbook.adapter.out.persistence.entity.BestBookJpaEntity;
import com.example.library.bestbook.domain.model.BestBook;
import org.springframework.stereotype.Component;

@Component
public class BestBookPersistenceMapper {
    public BestBookJpaEntity toJpaEntity(BestBook bestBook) {
        return new BestBookJpaEntity(
            bestBook.getId(),
            bestBook.getItemNo(),
            bestBook.getItemTitle(),
            bestBook.getRentCount()
        );
    }

    public BestBook toDomain(BestBookJpaEntity entity) {
        return new BestBook(
            entity.getId(),
            entity.getItemNo(),
            entity.getItemTitle(),
            entity.getRentCount()
        );
    }
}
