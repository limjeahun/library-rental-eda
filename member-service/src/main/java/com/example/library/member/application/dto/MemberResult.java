package com.example.library.member.application.dto;

import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

/**
 * 회원 도메인 모델을 외부 계층에 노출하기 위한 application 결과 DTO입니다.
 *
 * @param memberNo 회원 번호.
 * @param id 회원 ID.
 * @param name 회원 이름.
 * @param email 이메일 값.
 * @param authorities 회원에게 부여된 권한 목록.
 * @param point 회원 보유 포인트.
 */
public record MemberResult(Long memberNo, String id, String name, String email, List<UserRole> authorities, long point) {
    public MemberResult {
        authorities = List.copyOf(authorities);
    }

    @Override
    public List<UserRole> authorities() {
        return List.copyOf(authorities);
    }

    /**
     * 회원 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param member 변환할 회원 도메인 모델.
     * @return 회원 application 결과 DTO.
     */
    public static MemberResult from(Member member) {
        return new MemberResult(
            member.memberNo(),
            member.idName().id(),
            member.idName().name(),
            member.email().value(),
            member.authorities().stream().map(authority -> authority.role()).toList(),
            member.point().point()
        );
    }
}
