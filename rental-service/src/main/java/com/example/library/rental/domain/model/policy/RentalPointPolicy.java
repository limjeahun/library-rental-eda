package com.example.library.rental.domain.model.policy;

/**
 * rental-service 대여 흐름에서 적용되는 포인트 정책입니다.
 */
public enum RentalPointPolicy {
    RENT(10L),
    RETURN(10L);

    private final long point;

    RentalPointPolicy(long point) {
        this.point = point;
    }

    public long point() {
        return point;
    }
}
