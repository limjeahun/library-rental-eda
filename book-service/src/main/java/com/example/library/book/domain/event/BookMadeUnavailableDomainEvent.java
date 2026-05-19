package com.example.library.book.domain.event;

import java.time.Instant;

/**
 * 도서가 대여 불가능한 상태로 변경되었음을 표현하는 service-local domain event.
 */
public record BookMadeUnavailableDomainEvent(
    Instant occurredAt,
    Long bookNo,
    String title
) implements BookDomainEvent {
    public static BookMadeUnavailableDomainEvent of(Long bookNo, String title) {
        return new BookMadeUnavailableDomainEvent(
            Instant.now(),
            bookNo,
            title
        );
    }
}
