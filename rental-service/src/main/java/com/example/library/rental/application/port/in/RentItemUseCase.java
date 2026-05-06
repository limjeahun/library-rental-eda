package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.RentItemCommand;
import com.example.library.rental.application.dto.RentalCardResult;

/**
 * 대여카드에 도서를 추가하고 도서/회원/인기 도서 서비스가 받을 대여 이벤트를 발행.
 */
public interface RentItemUseCase {
    /**
     * 지정 회원에게 도서를 대여 처리.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 도서가 대여 목록에 추가되고 대여 이벤트가 발행된 대여카드를 반환.
     */
    RentalCardResult rentItem(RentItemCommand command);
}
