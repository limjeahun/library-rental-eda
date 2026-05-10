package com.example.library.member.application.port.out;

import com.example.library.member.domain.model.Member;

import java.util.Optional;

/**
 * 회원 번호로 회원 도메인 모델을 조회하는 outbound port.
 */
public interface LoadMemberPort {
    /**
     * 회원 번호로 회원 도메인 모델을 조회.
     *
     * @param memberNo 조회할 회원 번호.
     * @return 회원 번호에 해당하는 회원 도메인 모델을 반환.
     */
    Optional<Member> loadMember(long memberNo);
}
