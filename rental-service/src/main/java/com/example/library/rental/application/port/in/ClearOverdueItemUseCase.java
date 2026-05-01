package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.rental.domain.model.RentalCard;

/**
 * 연체료와 같은 포인트를 사용해 대여 정지를 해제하고 회원 포인트 차감 이벤트를 발행하는 application 계약입니다.
 */
public interface ClearOverdueItemUseCase {
    /**
     * 지정 회원의 연체료를 포인트로 정산하고 대여 가능 상태로 전환합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 연체료가 포인트로 정산되고 대여 가능 상태가 된 대여카드를 반환합니다.
     */
    RentalCard clearOverdue(IDName idName, long point);
}
