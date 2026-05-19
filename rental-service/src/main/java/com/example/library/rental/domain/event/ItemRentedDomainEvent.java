package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * 대여카드에 도서 대여가 반영된 뒤 발행되는 service-local domain event.
 */
public record ItemRentedDomainEvent(
        Instant occurredAt,
        RentalMember member,
        RentalItem item,
        long point
) implements RentalDomainEvent {
    public static ItemRentedDomainEvent of(RentalMember member, RentalItem item, long point) {
        return new ItemRentedDomainEvent(
                Instant.now(),
                member,
                item,
                point
        );
    }
}
