package com.example.library.rental.adapter.out.persistence;

import com.example.library.rental.application.port.out.CompensationIdempotencyPort;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * correlationId와 보상 타입 기준으로 보상 실행을 한 번만 허용하는 adapter입니다.
 */
@Repository
@RequiredArgsConstructor
public class CompensationIdempotencyPersistenceAdapter implements CompensationIdempotencyPort {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean markCompensated(String correlationId, String compensationType) {
        int inserted = jdbcTemplate.update(
            """
            insert ignore into rental_compensation_records
                (correlation_id, compensation_type, compensated_at)
            values (?, ?, ?)
            """,
            correlationId,
            compensationType,
            Timestamp.from(Instant.now())
        );
        return inserted == 1;
    }
}
