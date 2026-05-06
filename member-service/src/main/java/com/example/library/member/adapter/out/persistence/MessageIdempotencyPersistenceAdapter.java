package com.example.library.member.adapter.out.persistence;

import com.example.library.member.application.port.out.MessageIdempotencyPort;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 회원 서비스 메시지 처리 완료 여부를 MariaDB unique constraint로 보장하는 adapter입니다.
 */
@Repository
@RequiredArgsConstructor
public class MessageIdempotencyPersistenceAdapter implements MessageIdempotencyPort {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean markProcessed(String serviceName, String eventId, String correlationId, String messageType) {
        int inserted = jdbcTemplate.update(
            """
            insert ignore into processed_messages
                (service_name, event_id, correlation_id, message_type, processed_at)
            values (?, ?, ?, ?, ?)
            """,
            serviceName,
            eventId,
            correlationId,
            messageType,
            Timestamp.from(Instant.now())
        );
        return inserted == 1;
    }
}
