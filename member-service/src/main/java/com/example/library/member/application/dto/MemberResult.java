package com.example.library.member.application.dto;

import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

public record MemberResult(Long memberNo, String id, String name, String email, List<UserRole> authorites, long point) {
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
