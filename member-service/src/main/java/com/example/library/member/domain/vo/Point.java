package com.example.library.member.domain.vo;

/**
 * 회원 포인트의 적립과 사용 규칙을 캡슐화하는 값 객체입니다.
 */
public record Point(long point) {
    public Point {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }

    public Point savePoint(long amount) {
        validate(amount);
        return new Point(point + amount);
    }

    public Point usePoint(long amount) {
        validate(amount);
        if (point < amount) {
            throw new IllegalArgumentException("보유 포인트보다 많이 사용할 수 없습니다.");
        }
        return new Point(point - amount);
    }

    private static void validate(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }
}
