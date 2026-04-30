package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.MemberResult;

public interface MemberQueryUseCase {
    MemberResult getMember(long memberNo);

    MemberResult getMemberById(String id);
}
