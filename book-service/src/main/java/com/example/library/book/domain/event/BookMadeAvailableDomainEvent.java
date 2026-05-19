package com.example.library.book.domain.event;

import java.time.Instant;

/**
 * 도서가 대여 가능한 상태로 변경되었음을 표현하는 service-local domain event.
 */
public record BookMadeAvailableDomainEvent(
    Instant occurredAt,
    Long bookNo,
    String title
) implements BookDomainEvent {
    public static BookMadeAvailableDomainEvent of(Long bookNo, String title) {
        return new BookMadeAvailableDomainEvent(
            Instant.now(),
            bookNo,
            title
        );
    }
}
