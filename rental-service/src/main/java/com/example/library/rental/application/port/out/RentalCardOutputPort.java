package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;

/**
 * 대여 application 서비스가 대여카드를 저장하고 회원 ID로 조회할 때 사용하는 저장소 계약입니다.
 */
public interface RentalCardOutputPort {
    /**
     * 대여카드 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    RentalCard save(RentalCard rentalCard);

    /**
     * 회원 ID로 대여카드 도메인 모델을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원 ID에 해당하는 대여카드를 담은 Optional을 반환합니다.
     */
    Optional<RentalCard> loadRentalCard(String userId);
}
