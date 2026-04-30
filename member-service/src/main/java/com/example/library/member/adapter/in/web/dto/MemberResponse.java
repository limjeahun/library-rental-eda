package com.example.library.member.adapter.in.web.dto;

import com.example.library.member.application.dto.MemberResult;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

public record MemberResponse(Long memberNo, String id, String name, String email, List<UserRole> authorites, long point) {
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
