package com.example.library.rental.domain.vo;

/**
 * 연체료 포인트의 증가와 차감 규칙을 캡슐화하는 값 객체입니다.
 */
public record LateFee(long point) {
    public LateFee {
        validatePositiveOrZero(point);
    }

    public LateFee addPoint(long amount) {
        validatePositiveOrZero(amount);
        return new LateFee(point + amount);
    }

    public LateFee removePoint(long amount) {
        validatePositiveOrZero(amount);
        if (point < amount) {
            throw new IllegalArgumentException("차감할 연체료가 현재 연체료보다 큽니다.");
        }
        return new LateFee(point - amount);
    }

    private static void validatePositiveOrZero(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }
}
