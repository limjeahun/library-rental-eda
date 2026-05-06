package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.RentItemResult;
import com.example.library.rental.application.dto.RentalCardResult;
import com.example.library.rental.application.dto.ReturnItemResult;
import java.util.List;

/**
 * 회원 ID로 대여카드, 현재 대여 목록, 반납 완료 목록을 조회.
 */
public interface RentalCardQueryUseCase {
    /**
     * 회원 ID로 대여카드를 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 회원 ID에 해당하는 대여카드를 반환.
     */
    RentalCardResult getRentalCard(String userId);

    /**
     * 회원 ID로 현재 대여 중인 도서 목록을 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 현재 대여 중인 도서 목록을 반환.
     */
    List<RentItemResult> getRentItems(String userId);

    /**
     * 회원 ID로 반납 완료된 도서 목록을 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 반납 완료된 도서 목록을 반환.
     */
    List<ReturnItemResult> getReturnItems(String userId);
}
