package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.application.dto.MemberResult;

/**
 * 회원 ID, 이름, 이메일, 비밀번호를 받아 기본 권한과 0포인트 회원을 등록하는 application 계약입니다.
 */
public interface AddMemberUseCase {
    /**
     * 회원 등록 command를 처리하고 등록 결과를 반환합니다.
     *
     * @param command 등록할 회원 ID, 이름, 이메일, 비밀번호를 담은 application command입니다.
     * @return 등록된 회원의 번호, ID, 이름, 이메일, 포인트를 담은 결과 DTO를 반환합니다.
     */
    MemberResult addMember(AddMemberCommand command);
}
