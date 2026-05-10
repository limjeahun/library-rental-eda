package com.example.library.member.application.port.out;

import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.domain.model.Member;

import java.util.Optional;

/**
 * 회원 ID 값으로 회원 도메인 모델을 조회하는 outbound port.
 */
public interface LoadMemberByIdNamePort {
    /**
     * 회원 ID 값으로 회원 도메인 모델을 조회.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @return 회원 ID에 해당하는 회원 도메인 모델을 반환.
     */
    Optional<Member> loadMemberByIdName(MemberIdentity idName);
}
