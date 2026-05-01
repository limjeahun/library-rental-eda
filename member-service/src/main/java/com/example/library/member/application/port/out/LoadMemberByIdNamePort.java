package com.example.library.member.application.port.out;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

/**
 * 회원 ID 값으로 회원 도메인 모델을 조회하는 outbound port입니다.
 */
public interface LoadMemberByIdNamePort {
    /**
     * 회원 ID 값으로 회원 도메인 모델을 조회합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @return 회원 ID에 해당하는 회원 도메인 모델을 반환합니다.
     */
    Member loadMemberByIdName(IDName idName);
}
