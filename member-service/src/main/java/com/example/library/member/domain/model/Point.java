package com.example.library.member.domain.model;

/**
 * 회원 포인트의 적립과 사용 규칙을 캡슐화하는 값 객체입니다.
 */
public class Point {
    private long point;

    /**
     * 0 이상의 보유 포인트 값을 생성합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public Point(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
        this.point = point;
    }

    /**
     * 0 이상의 포인트를 적립하고 변경된 잔액을 반환합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 적립 후 보유 포인트 잔액을 반환합니다.
     */
    public long savePoint(long point) {
        validate(point);
        this.point += point;
        return this.point;
    }

    /**
     * 보유 포인트 한도 안에서 포인트를 차감하고 변경된 잔액을 반환합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 차감 후 보유 포인트 잔액을 반환합니다.
     */
    public long usePoint(long point) {
        validate(point);
        if (this.point < point) {
            throw new IllegalArgumentException("보유 포인트보다 많이 사용할 수 없습니다.");
        }
        this.point -= point;
        return this.point;
    }

    /**
     * 포인트 변경 값이 음수가 아닌지 검증합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    private void validate(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }

    /**
     * 현재 보유 포인트를 반환합니다.
     *
     * @return 현재 저장된 포인트 금액을 반환합니다.
     */
    public long getPoint() {
        return point;
    }
}
