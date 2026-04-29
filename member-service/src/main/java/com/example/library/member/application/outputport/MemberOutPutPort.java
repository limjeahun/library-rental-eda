package com.example.library.member.application.outputport;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

public interface MemberOutPutPort {
    Member loadMember(long memberNo);

    Member loadMemberByIdName(IDName idName);

    Member saveMember(Member member);
}
