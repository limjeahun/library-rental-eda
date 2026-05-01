package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.dto.MemberResult;

/**
 * 연체료 정산이나 보상 흐름에서 회원 포인트를 차감하는 application 계약입니다.
 */
public interface UsePointUseCase {
    /**
     * 지정 회원의 포인트를 차감합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     * @return 차감 후 보유 포인트 잔액을 반환합니다.
     */
    MemberResult usePoint(ChangePointCommand command);
}
