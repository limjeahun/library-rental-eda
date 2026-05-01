package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCard;

/**
 * 대여카드 도메인 모델을 저장하는 outbound port입니다.
 */
public interface SaveRentalCardPort {
    /**
     * 대여카드 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    RentalCard save(RentalCard rentalCard);
}
