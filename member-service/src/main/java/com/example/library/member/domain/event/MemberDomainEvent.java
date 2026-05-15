package com.example.library.member.domain.event;

import java.time.Instant;

/**
 * member-service 내부 aggregate 에서 발생한 service-local domain event 의 공통 marker interface.
 */
public interface MemberDomainEvent {
    default Instant occurredAt() {
        return Instant.now();
    }
}
