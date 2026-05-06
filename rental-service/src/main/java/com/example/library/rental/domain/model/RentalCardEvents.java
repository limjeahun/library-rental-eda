package com.example.library.rental.domain.model;

import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;

/**
 * RentalCard aggregate 상태 변경 뒤 application 계층이 발행할 수 있는 service-local domain events입니다.
 */
public final class RentalCardEvents {
    private RentalCardEvents() {
    }

    public record ItemRentedDomainEvent(RentalMember idName, RentalItem item, long point) {
    }

    public record ItemReturnedDomainEvent(RentalMember idName, RentalItem item, long point) {
    }

    public record OverdueClearedDomainEvent(RentalMember idName, long point) {
    }

    public record ItemRentCanceledDomainEvent(RentalMember idName, RentalItem item, long point) {
    }

    public record ItemReturnCanceledDomainEvent(RentalMember idName, RentalItem item, long point) {
    }

    public record OverdueClearCanceledDomainEvent(RentalMember idName, long point) {
    }
}
