package com.example.library.member.application.port.in;

import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.dto.MemberResult;

public interface UsePointUseCase {
    MemberResult usePoint(ChangePointCommand command);
}
