package com.example.library.member.application.port.out;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

/**
 * 회원 application 서비스가 회원 도메인 모델의 저장과 조회를 요청할 때 사용하는 저장소 계약입니다.
 */
public interface MemberOutputPort {
    /**
     * 회원 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param member 저장하거나 응답으로 변환할 회원 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 포인트가 반영된 회원 도메인 모델을 반환합니다.
     */
    Member saveMember(Member member);

    /**
     * 회원 번호로 회원 도메인 모델을 조회합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @return 회원 번호에 해당하는 회원 도메인 모델을 반환합니다.
     */
    Member loadMember(long memberNo);

    /**
     * 회원 ID 값으로 회원 도메인 모델을 조회합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @return 회원 ID에 해당하는 회원 도메인 모델을 반환합니다.
     */
    Member loadMemberByIdName(IDName idName);
}
