package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.application.dto.MemberResult;

/**
 * 회원 ID, 이름, 이메일, 비밀번호를 받아 기본 권한과 0포인트 회원을 등록하는 application 계약.
 */
public interface AddMemberUseCase {
    /**
     * 새 회원 등록을 처리.
     *
     * @param command 등록할 회원 ID, 이름, 이메일, 비밀번호를 담은 application command.
     * @return 등록된 회원의 번호, ID, 이름, 이메일, 포인트를 담은 결과 DTO.
     */
    MemberResult addMember(AddMemberCommand command);
}
