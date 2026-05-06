package com.example.library.rental.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * correlationId와 보상 타입 기준으로 비즈니스 보상 실행 이력을 저장하는 JPA 엔티티입니다.
 */
@Entity
@Table(
    name = "rental_compensation_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rental_compensation_correlation_type",
        columnNames = {"correlation_id", "compensation_type"}
    )
)
public class CompensationRecordJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, length = 120)
    private String correlationId;

    @Column(name = "compensation_type", nullable = false, length = 120)
    private String compensationType;

    @Column(name = "compensated_at", nullable = false)
    private Instant compensatedAt;

    protected CompensationRecordJpaEntity() {
    }

    public CompensationRecordJpaEntity(String correlationId, String compensationType) {
        this.correlationId = correlationId;
        this.compensationType = compensationType;
        this.compensatedAt = Instant.now();
    }
}
