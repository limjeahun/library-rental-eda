package com.example.library.rental.application.port.in;

import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.util.List;

/**
 * 회원 ID로 대여카드, 현재 대여 목록, 반납 완료 목록을 조회하는 application 계약입니다.
 */
public interface RentalCardQueryUseCase {
    /**
     * 회원 ID로 대여카드를 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원 ID에 해당하는 대여카드를 반환합니다.
     */
    RentalCard getRentalCard(String userId);

    /**
     * 회원 ID로 현재 대여 중인 도서 목록을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 현재 대여 중인 도서 목록을 반환합니다.
     */
    List<RentItem> getRentItems(String userId);

    /**
     * 회원 ID로 반납 완료된 도서 목록을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 반납 완료된 도서 목록을 반환합니다.
     */
    List<ReturnItem> getReturnItems(String userId);
}
