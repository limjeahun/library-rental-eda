package com.example.library.book.adapter.out.persistence;

import com.example.library.book.application.port.out.MessageIdempotencyPort;
import com.example.library.common.event.InboundMessageType;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 도서 서비스 메시지 처리 완료 여부를 MariaDB unique constraint로 보장하는 adapter입니다.
 */
@Repository
@RequiredArgsConstructor
public class MessageIdempotencyPersistenceAdapter implements MessageIdempotencyPort {
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Override
    public boolean markProcessed(String eventId, String correlationId, InboundMessageType messageType) {
        int inserted = jdbcTemplate.update(
            """
            insert ignore into processed_messages
                (service_name, event_id, correlation_id, message_type, processed_at)
            values (?, ?, ?, ?, ?)
            """,
            serviceName(),
            eventId,
            correlationId,
            messageType.name(),
            Timestamp.from(Instant.now())
        );
        return inserted == 1;
    }

    private String serviceName() {
        return environment.getRequiredProperty("spring.application.name");
    }
}
