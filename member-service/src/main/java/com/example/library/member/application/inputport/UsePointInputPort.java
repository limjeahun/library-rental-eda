package com.example.library.member.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.outputport.MemberOutPutPort;
import com.example.library.member.application.usecase.UsePointUsecase;
import com.example.library.member.domain.model.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsePointInputPort implements UsePointUsecase {
    private final MemberOutPutPort memberOutPutPort;

    public UsePointInputPort(MemberOutPutPort memberOutPutPort) {
        this.memberOutPutPort = memberOutPutPort;
    }

    @Override
    public Member userPoint(IDName idName, long point) {
        Member member = memberOutPutPort.loadMemberByIdName(idName);
        member.usePoint(point);
        return memberOutPutPort.saveMember(member);
    }
}
