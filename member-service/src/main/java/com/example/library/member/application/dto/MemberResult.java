package com.example.library.member.application.dto;

import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

/**
 * 회원 도메인 모델을 외부 계층에 노출하기 위한 application 결과 DTO입니다.
 *
 * @param memberNo 조회할 회원 번호입니다.
 * @param id 조회하거나 포인트를 변경할 회원 ID입니다.
 * @param name 회원 이름입니다.
 * @param email 저장하거나 검증할 이메일 값입니다.
 * @param authorites 회원에게 부여된 권한 목록입니다.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record MemberResult(Long memberNo, String id, String name, String email, List<UserRole> authorites, long point) {
    /**
     * 회원 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param member 저장하거나 응답으로 변환할 회원 도메인 모델입니다.
     * @return 도메인 모델 또는 application 결과 DTO를 HTTP 응답 DTO로 변환해 반환합니다.
     */
    public static MemberResult from(Member member) {
        return new MemberResult(
            member.getMemberNo(),
            member.getIdName().getId(),
            member.getIdName().getName(),
            member.getEmail().getValue(),
            member.getAuthorites().stream().map(authority -> authority.getRole()).toList(),
            member.getPoint().getPoint()
        );
    }
}
