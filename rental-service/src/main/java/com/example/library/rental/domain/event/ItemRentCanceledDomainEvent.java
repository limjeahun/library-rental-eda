package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;

/**
 * 대여 보상으로 대여 취소가 반영된 뒤 발행되는 service-local domain event입니다.
 */
public record ItemRentCanceledDomainEvent(RentalMember idName, RentalItem item, long point) {
}
