package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointUseCommand;

/**
 * 대여/반납 포인트 적립, 연체료 포인트 차감, 보상 포인트 차감 메시지를 처리하는 application 계약입니다.
 */
public interface HandleMemberEventUseCase {
    /**
     * 도서 대여 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 대여 포인트 적립 application command입니다.
     */
    void handleRent(MemberPointSaveCommand command);

    /**
     * 도서 반납 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 반납 포인트 적립 application command입니다.
     */
    void handleReturn(MemberPointSaveCommand command);

    /**
     * 연체 해제 이벤트를 처리합니다.
     *
     * @param command 처리할 연체 해제 포인트 차감 application command입니다.
     */
    void handleOverdueClear(MemberOverdueClearCommand command);

    /**
     * 포인트 사용 command를 처리합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 application command입니다.
     */
    void handlePointUse(MemberPointUseCommand command);
}
