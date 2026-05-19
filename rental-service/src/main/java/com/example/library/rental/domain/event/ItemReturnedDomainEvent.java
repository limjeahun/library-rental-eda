package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * 대여카드에 도서 반납이 반영된 뒤 발행되는 service-local domain event.
 */
public record ItemReturnedDomainEvent(
        Instant occurredAt,
        RentalMember member,
        RentalItem item,
        long point
) implements RentalDomainEvent {
    public static ItemReturnedDomainEvent of(RentalMember member, RentalItem item, long point) {
        return new ItemReturnedDomainEvent(
                Instant.now(),
                member,
                item,
                point
        );
    }
}
