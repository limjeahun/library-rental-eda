package com.example.library.bestbook.adapter.out.persistence;

import com.example.library.bestbook.adapter.out.persistence.document.ProcessedMessageDocument;
import com.example.library.bestbook.adapter.out.persistence.repository.ProcessedMessageMongoRepository;
import com.example.library.bestbook.application.port.out.MessageIdempotencyPort;
import com.example.library.common.event.InboundMessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 인기 도서 서비스 메시지 처리 완료 여부를 MongoDB unique index로 보장하는 adapter입니다.
 *
 * <p>Kafka 재전달에 대비해 {@code serviceName + eventId}를 MongoDB unique index로 기록합니다.
 */
@Repository
@RequiredArgsConstructor
public class MessageIdempotencyPersistenceAdapter implements MessageIdempotencyPort {
    private final ProcessedMessageMongoRepository repository;
    private final Environment environment;

    @Override
    public boolean markProcessed(String eventId, String correlationId, InboundMessageType messageType) {
        String serviceName = serviceName();
        if (repository.existsByServiceNameAndEventId(serviceName, eventId)) {
            return false;
        }
        try {
            // exists와 save 사이의 경쟁은 unique index 충돌로 한 번 더 막습니다.
            repository.save(new ProcessedMessageDocument(serviceName, eventId, correlationId, messageType.name()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    private String serviceName() {
        return environment.getRequiredProperty("spring.application.name");
    }
}
