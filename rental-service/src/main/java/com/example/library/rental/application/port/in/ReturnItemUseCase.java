package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.RentalCardResult;
import com.example.library.rental.application.dto.ReturnItemCommand;

/**
 * 대여 중인 도서를 반납 목록으로 이동하고 도서/회원 서비스가 받을 반납 이벤트를 발행.
 */
public interface ReturnItemUseCase {
    /**
     * 지정 회원의 대여 도서를 반납 처리.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 도서가 반납 목록으로 이동하고 반납 이벤트가 발행된 대여카드를 반환.
     */
    RentalCardResult returnItem(ReturnItemCommand command);
}
