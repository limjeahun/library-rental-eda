package com.example.library.book.domain.event;

import java.time.Instant;

/**
 * book-service 내부 aggregate 에서 발생한 service-local domain event 의 공통 marker interface.
 */
public interface BookDomainEvent {
    Instant occurredAt();
}
