package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;

/**
 * 대여카드에 도서 반납이 반영된 뒤 발행되는 service-local domain event.
 */
public record ItemReturnedDomainEvent(RentalMember member, RentalItem item, long point) implements RentalDomainEvent {
}
