package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.dto.MemberResult;

/**
 * 대여/반납 완료로 회원 포인트를 적립하는 application 계약입니다.
 */
public interface SavePointUseCase {
    /**
     * 지정 회원에게 포인트를 적립합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     * @return 적립 후 보유 포인트 잔액을 반환합니다.
     */
    MemberResult savePoint(ChangePointCommand command);
}
