package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.ClearOverdueCommand;
import com.example.library.rental.application.dto.RentalCardResult;

/**
 * 연체료와 같은 포인트를 사용해 대여 정지를 해제하고 회원 포인트 차감 이벤트를 발행.
 */
public interface ClearOverdueItemUseCase {
    /**
     * 지정 회원의 연체료를 포인트로 정산하고 대여 가능 상태로 전환.
     *
     * @param command 대상 회원과 포인트 정보를 담은 입력 command.
     * @return 연체료가 포인트로 정산되고 대여 가능 상태가 된 대여카드를 반환.
     */
    RentalCardResult clearOverdue(ClearOverdueCommand command);
}
