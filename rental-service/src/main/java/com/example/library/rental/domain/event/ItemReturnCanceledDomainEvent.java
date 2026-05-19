package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * 반납 보상으로 반납 취소가 반영된 뒤 발행되는 service-local domain event.
 */
public record ItemReturnCanceledDomainEvent(
        Instant occurredAt,
        RentalMember member,
        RentalItem item,
        long point
) implements RentalDomainEvent {
    public static ItemReturnCanceledDomainEvent of(RentalMember member, RentalItem item, long point) {
        return new ItemReturnCanceledDomainEvent(
                Instant.now(),
                member,
                item,
                point
        );
    }
}
