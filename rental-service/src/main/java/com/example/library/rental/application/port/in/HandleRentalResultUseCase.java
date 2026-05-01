package com.example.library.rental.application.port.in;

import com.example.library.common.event.EventResult;

/**
 * 도서/회원 서비스 결과 이벤트를 받아 실패한 RENT/RETURN/OVERDUE 흐름의 보상 여부를 판단하는 application 계약입니다.
 */
public interface HandleRentalResultUseCase {
    /**
     * 참여 서비스 처리 결과를 받아 보상 여부를 판단합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    void handle(EventResult result);
}
