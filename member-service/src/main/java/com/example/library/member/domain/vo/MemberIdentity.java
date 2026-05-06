package com.example.library.member.domain.vo;

/**
 * member-service 내부에서 회원 ID와 이름을 표현하는 값 객체입니다.
 */
public record MemberIdentity(String id, String name) {
    public MemberIdentity {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("회원 ID는 비어 있을 수 없습니다.");
        }
    }
}
