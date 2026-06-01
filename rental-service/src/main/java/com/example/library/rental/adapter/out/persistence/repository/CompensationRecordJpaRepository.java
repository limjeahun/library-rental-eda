package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.CompensationRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * rental-service 비즈니스 보상 실행 이력을 저장하고 조회하는 Spring Data repository.
 */
public interface CompensationRecordJpaRepository extends JpaRepository<CompensationRecordJpaEntity, Long> {
    @Modifying
    @Query(
        value = """
            insert ignore into rental_compensation_records (
                correlation_id,
                compensation_type,
                compensated_at
            )
            values (
                :correlationId,
                :compensationType,
                current_timestamp(6)
            )
            """,
        nativeQuery = true
    )
    int insertIgnore(
        @Param("correlationId") String correlationId,
        @Param("compensationType") String compensationType
    );

    long countByCorrelationId(String correlationId);
}
