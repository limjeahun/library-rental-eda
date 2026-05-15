package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalMember;

/**
 * 연체 해제 보상으로 대여카드 연체 상태가 복구된 뒤 발행되는 service-local domain event.
 */
public record OverdueClearCanceledDomainEvent(RentalMember member, long point) implements RentalDomainEvent {
}
