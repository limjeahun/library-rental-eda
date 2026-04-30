package com.example.library.member.application.port.out;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

public interface MemberOutputPort {
    Member saveMember(Member member);

    Member loadMember(long memberNo);

    Member loadMemberByIdName(IDName idName);
}
