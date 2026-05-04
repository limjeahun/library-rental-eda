package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.rental.domain.model.RentalCard;

/**
 * 회원별 대여카드를 새로 만드는 Use Case
 */
public interface CreateRentalCardUseCase {
    /**
     * 회원 식별 값으로 대여카드를 생성하거나 조회.
     *
     * @param creator 대여카드를 생성할 회원의 식별 값.
     * @return 기존 대여카드 또는 새로 생성된 대여카드를 반환.
     */
    RentalCard createRentalCard(IDName creator);
}
