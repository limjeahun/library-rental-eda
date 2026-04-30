package com.example.library.bestbook.adapter.out.persistence.repository;

import com.example.library.bestbook.adapter.out.persistence.entity.BestBookJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BestBookJpaRepository extends JpaRepository<BestBookJpaEntity, Long> {
    Optional<BestBookJpaEntity> findByItemNo(Long itemNo);
}
