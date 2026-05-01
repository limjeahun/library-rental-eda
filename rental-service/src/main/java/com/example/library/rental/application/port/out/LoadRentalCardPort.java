package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;

/**
 * 회원 ID로 대여카드 도메인 모델을 조회하는 outbound port입니다.
 */
public interface LoadRentalCardPort {
    /**
     * 회원 ID로 대여카드 도메인 모델을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원 ID에 해당하는 대여카드를 담은 Optional을 반환합니다.
     */
    Optional<RentalCard> loadRentalCard(String userId);
}
