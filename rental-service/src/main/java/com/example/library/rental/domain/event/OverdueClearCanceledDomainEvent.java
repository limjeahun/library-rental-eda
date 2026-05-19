package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;

/**
 * 연체 해제 보상으로 대여카드 연체 상태가 복구된 뒤 발행되는 service-local domain event.
 */
public record OverdueClearCanceledDomainEvent(
        Instant occurredAt,
        RentalMember member,
        long point
) implements RentalDomainEvent {
    public static OverdueClearCanceledDomainEvent of(RentalMember member, long point) {
        return new OverdueClearCanceledDomainEvent(
                Instant.now(),
                member,
                point
        );
    }
}
