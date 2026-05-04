package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCard;

/**
 * 대여카드 도메인 모델을 저장.
 */
public interface SaveRentalCardPort {
    /**
     * 대여카드 도메인 모델을 저장하고 저장된 모델을 반환.
     *
     * @param rentalCard 저장하거나 응답 DTO 로 변환할 대여카드 도메인 모델.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환.
     */
    RentalCard save(RentalCard rentalCard);
}
