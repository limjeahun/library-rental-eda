package com.example.library.book.domain.event;

/**
 * 도서가 대여 가능한 상태로 변경되었음을 표현하는 service-local domain event.
 */
public record BookMadeAvailableDomainEvent(
    Long bookNo,
    String title
) implements BookDomainEvent {
}
