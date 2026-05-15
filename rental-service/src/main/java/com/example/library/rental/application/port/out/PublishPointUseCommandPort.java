package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.vo.RentalMember;

/**
 * 보상 흐름에서 포인트 사용 command를 발행하는 outbound port입니다.
 */
public interface PublishPointUseCommandPort {
    /**
     * 대여 실패 보상으로 회원에게 적립된 대여 포인트 차감을 요청합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot입니다.
     * @param point 차감할 대여 포인트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
     */
    void publishRentPointUseCommand(RentalMember member, long point, String correlationId);

    /**
     * 반납 실패 보상으로 회원에게 적립된 반납 포인트 차감을 요청합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot입니다.
     * @param point 차감할 반납 포인트입니다.
     * @param correlationId 반납 흐름 전체를 추적하는 상관관계 ID입니다.
     */
    void publishReturnPointUseCommand(RentalMember member, long point, String correlationId);
}
