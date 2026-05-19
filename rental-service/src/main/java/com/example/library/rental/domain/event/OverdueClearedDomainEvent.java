package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * 대여카드에 연체 해제가 반영된 뒤 발행되는 service-local domain event.
 */
public record OverdueClearedDomainEvent(
        Instant occurredAt,
        RentalMember member,
        long point
) implements RentalDomainEvent {
    public static OverdueClearedDomainEvent of(RentalMember member, long point) {
        return new OverdueClearedDomainEvent(
                Instant.now(),
                member,
                point
        );
    }
}
