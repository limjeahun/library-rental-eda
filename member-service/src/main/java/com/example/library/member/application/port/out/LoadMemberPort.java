package com.example.library.member.application.port.out;

import com.example.library.member.domain.model.Member;

/**
 * 회원 번호로 회원 도메인 모델을 조회하는 outbound port입니다.
 */
public interface LoadMemberPort {
    /**
     * 회원 번호로 회원 도메인 모델을 조회합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @return 회원 번호에 해당하는 회원 도메인 모델을 반환합니다.
     */
    Member loadMember(long memberNo);
}
