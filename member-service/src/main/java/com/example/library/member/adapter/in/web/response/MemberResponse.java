package com.example.library.member.adapter.in.web.response;

import com.example.library.member.application.dto.MemberResult;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

/**
 * 회원 API 응답으로 반환하는 HTTP DTO입니다.
 *
 * @param memberNo 조회할 회원 번호입니다.
 * @param id 조회하거나 포인트를 변경할 회원 ID입니다.
 * @param name 회원 이름입니다.
 * @param email 저장하거나 검증할 이메일 값입니다.
 * @param authorites 회원에게 부여된 권한 목록입니다.
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record MemberResponse(Long memberNo, String id, String name, String email, List<UserRole> authorites, long point) {
    /**
     * application 결과 DTO를 HTTP 응답 DTO로 변환합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
            result.memberNo(),
            result.id(),
            result.name(),
            result.email(),
            result.authorites(),
            result.point()
        );
    }
}
