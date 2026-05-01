package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.MemberResult;

/**
 * 회원 번호 또는 회원 ID로 회원 정보를 조회하는 application 계약입니다.
 */
public interface MemberQueryUseCase {
    /**
     * 회원 번호로 회원을 조회합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @return 회원 번호에 해당하는 회원 결과 DTO를 반환합니다.
     */
    MemberResult getMember(long memberNo);

    /**
     * 회원 로그인 ID로 회원을 조회합니다.
     *
     * @param id 회원 로그인 ID입니다.
     * @return 회원 ID에 해당하는 회원 결과 DTO를 반환합니다.
     */
    MemberResult getMemberById(String id);
}
