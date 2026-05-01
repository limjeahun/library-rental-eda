package com.example.library.member.application.port.out;

import com.example.library.member.domain.model.Member;

/**
 * 회원 도메인 모델을 저장하는 outbound port입니다.
 */
public interface SaveMemberPort {
    /**
     * 회원 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param member 저장하거나 응답으로 변환할 회원 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 포인트가 반영된 회원 도메인 모델을 반환합니다.
     */
    Member saveMember(Member member);
}
