package com.example.library.member.adapter.in.web.response;

import com.example.library.member.application.dto.MemberResult;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

/**
 * 회원 API 응답으로 반환하는 HTTP DTO입니다.
 *
 * @param memberNo 회원 번호.
 * @param id 회원 ID.
 * @param name 회원 이름.
 * @param email 이메일 값.
 * @param authorities 회원에게 부여된 권한 목록.
 * @param point 회원 보유 포인트.
 */
public record MemberResponse(Long memberNo, String id, String name, String email, List<UserRole> authorities, long point) {
    /**
     * application 결과를 HTTP 응답 형태로 옮깁니다.
     *
     * @param result 회원 application 결과 DTO.
     * @return 클라이언트에 반환할 HTTP 응답 DTO.
     */
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
            result.memberNo(),
            result.id(),
            result.name(),
            result.email(),
            result.authorities(),
            result.point()
        );
    }
}
