package com.example.library.rental.domain.model;

public class LateFee {
    private long point;

    public LateFee() {
    }

    public LateFee(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("연체료는 0 이상이어야 합니다.");
        }
        this.point = point;
    }

    public void addPoint(long point) {
        validatePositiveOrZero(point);
        this.point += point;
    }

    public void removePoint(long point) {
        validatePositiveOrZero(point);
        if (this.point < point) {
            throw new IllegalArgumentException("차감할 연체료가 현재 연체료보다 큽니다.");
        }
        this.point -= point;
    }

    private void validatePositiveOrZero(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
