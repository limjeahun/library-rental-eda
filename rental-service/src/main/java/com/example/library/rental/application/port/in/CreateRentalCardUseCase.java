package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.CreateRentalCardCommand;
import com.example.library.rental.application.dto.RentalCardResult;

/**
 * 회원별 대여카드를 새로 만드는 Use Case
 */
public interface CreateRentalCardUseCase {
    /**
     * 회원 생성 command로 대여카드를 생성하거나 조회.
     *
     * @param command 대여카드를 생성할 회원 입력 command.
     * @return 기존 대여카드 또는 새로 생성된 대여카드를 반환.
     */
    RentalCardResult createRentalCard(CreateRentalCardCommand command);
}
