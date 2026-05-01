package com.example.library.rental.domain.model;

/**
 * 연체료 포인트의 증가와 차감 규칙을 캡슐화하는 값 객체입니다.
 */
public class LateFee {
    private long point;

    /**
     * 프레임워크 바인딩과 영속성 복원을 위한 기본 생성자입니다.
     */
    public LateFee() {
    }

    /**
     * 0 이상의 연체료 포인트 값을 생성합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public LateFee(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("연체료는 0 이상이어야 합니다.");
        }
        this.point = point;
    }

    /**
     * 연체료에 지정한 포인트를 더합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public void addPoint(long point) {
        validatePositiveOrZero(point);
        this.point += point;
    }

    /**
     * 현재 연체료 한도 안에서 지정한 포인트를 차감합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public void removePoint(long point) {
        validatePositiveOrZero(point);
        if (this.point < point) {
            throw new IllegalArgumentException("차감할 연체료가 현재 연체료보다 큽니다.");
        }
        this.point -= point;
    }

    /**
     * 포인트 변경 값이 음수가 아닌지 검증합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    private void validatePositiveOrZero(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
    }

    /**
     * 현재 연체료 포인트를 반환합니다.
     *
     * @return 현재 저장된 포인트 금액을 반환합니다.
     */
    public long getPoint() {
        return point;
    }

    /**
     * 현재 연체료 포인트를 설정합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public void setPoint(long point) {
        this.point = point;
    }
}
