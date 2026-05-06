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
 * 대여 서비스가 이미 처리한 Kafka 메시지를 MariaDB에 기록하는 JPA 엔티티입니다.
 */
@Entity
@Table(
    name = "processed_messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rental_processed_message_service_event",
        columnNames = {"service_name", "event_id"}
    )
)
public class ProcessedMessageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 80)
    private String serviceName;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "message_type", nullable = false, length = 120)
    private String messageType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedMessageJpaEntity() {
    }

    public ProcessedMessageJpaEntity(String serviceName, String eventId, String correlationId, String messageType) {
        this.serviceName = serviceName;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.messageType = messageType;
        this.processedAt = Instant.now();
    }
}
