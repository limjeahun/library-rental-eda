package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;

/**
 * 반납 보상으로 반납 취소가 반영된 뒤 발행되는 service-local domain event.
 */
public record ItemReturnCanceledDomainEvent(RentalMember member, RentalItem item, long point) implements RentalDomainEvent {
}
