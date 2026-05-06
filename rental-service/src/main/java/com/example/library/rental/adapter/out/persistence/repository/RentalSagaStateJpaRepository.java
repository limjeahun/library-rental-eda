package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.RentalSagaStateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * rental-service SAGA 추적 상태를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface RentalSagaStateJpaRepository extends JpaRepository<RentalSagaStateJpaEntity, String> {
}
