package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.CompensationRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * rental-service 비즈니스 보상 실행 이력을 저장하고 조회하는 Spring Data repository.
 */
public interface CompensationRecordJpaRepository extends JpaRepository<CompensationRecordJpaEntity, Long> {
}
