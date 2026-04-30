package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.application.dto.MemberResult;

public interface AddMemberUseCase {
    MemberResult addMember(AddMemberCommand command);
}
