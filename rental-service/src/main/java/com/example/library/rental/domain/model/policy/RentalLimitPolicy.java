package com.example.library.rental.domain.model.policy;

/**
 * 대여카드가 동시에 보유할 수 있는 대여 도서 수 정책입니다.
 */
public enum RentalLimitPolicy {
    STANDARD(5);

    private final int maxRentalCount;

    RentalLimitPolicy(int maxRentalCount) {
        this.maxRentalCount = maxRentalCount;
    }

    public boolean canRent(int currentRentalCount) {
        return currentRentalCount < maxRentalCount;
    }

    public int maxRentalCount() {
        return maxRentalCount;
    }
}
