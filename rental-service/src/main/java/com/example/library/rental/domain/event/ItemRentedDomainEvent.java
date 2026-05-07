package com.example.library.rental.domain.event;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;

/**
 * 대여카드에 도서 대여가 반영된 뒤 발행되는 service-local domain event입니다.
 */
public record ItemRentedDomainEvent(RentalMember idName, RentalItem item, long point) {
}
