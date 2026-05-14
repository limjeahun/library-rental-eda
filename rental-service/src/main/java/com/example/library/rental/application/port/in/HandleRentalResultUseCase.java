package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.RentalResultCommand;

/**
 * 도서/회원 서비스 결과 이벤트를 받아 실패한 RENT/RETURN/OVERDUE 흐름의 보상 여부를 판단.
 */
public interface HandleRentalResultUseCase {
    /**
     * 참여 서비스 처리 결과를 받아 보상 여부를 판단.
     *
     * @param command 처리할 참여 서비스 결과 application command.
     */
    void handle(RentalResultCommand command);
}
