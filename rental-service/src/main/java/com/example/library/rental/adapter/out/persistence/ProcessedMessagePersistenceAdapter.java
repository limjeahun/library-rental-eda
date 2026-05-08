package com.example.library.rental.adapter.out.persistence;

import com.example.library.common.event.InboundMessageType;
import com.example.library.rental.adapter.out.persistence.entity.ProcessedMessageJpaEntity;
import com.example.library.rental.adapter.out.persistence.repository.ProcessedMessageJpaRepository;
import com.example.library.rental.application.port.out.MessageIdempotencyPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * 대여 서비스가 처리 완료한 Kafka 메시지를 저장소에 기록하는 outbound persistence adapter입니다.
 */
@Repository
public class ProcessedMessagePersistenceAdapter implements MessageIdempotencyPort {
    private final ProcessedMessageJpaRepository repository;
    private final String serviceName;

    public ProcessedMessagePersistenceAdapter(
        ProcessedMessageJpaRepository repository,
        @Value("${spring.application.name}") String serviceName
    ) {
        this.repository = repository;
        this.serviceName = serviceName;
    }

    @Override
    public boolean markProcessed(String eventId, String correlationId, InboundMessageType messageType) {
        validate(eventId, messageType);
        try {
            repository.saveAndFlush(
                new ProcessedMessageJpaEntity(serviceName, eventId, correlationId, messageType.name())
            );
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private void validate(String eventId, InboundMessageType messageType) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName은 비어 있을 수 없습니다.");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId는 비어 있을 수 없습니다.");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("messageType은 null일 수 없습니다.");
        }
    }
}
