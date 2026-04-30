package com.example.library.member.domain.model;

public class Point {
    private long point;

    public Point(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
        this.point = point;
    }

    public long savePoint(long point) {
        validate(point);
        this.point += point;
        return this.point;
    }

    public long usePoint(long point) {
        validate(point);
        if (this.point < point) {
            throw new IllegalArgumentException("보유 포인트보다 많이 사용할 수 없습니다.");
        }
        this.point -= point;
        return this.point;
    }

    private void validate(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }

    public long getPoint() {
        return point;
    }
}
