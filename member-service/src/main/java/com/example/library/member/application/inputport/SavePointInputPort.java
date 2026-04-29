package com.example.library.member.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.outputport.MemberOutPutPort;
import com.example.library.member.application.usecase.SavePointUsecase;
import com.example.library.member.domain.model.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavePointInputPort implements SavePointUsecase {
    private final MemberOutPutPort memberOutPutPort;

    public SavePointInputPort(MemberOutPutPort memberOutPutPort) {
        this.memberOutPutPort = memberOutPutPort;
    }

    @Override
    public Member savePoint(IDName idName, long point) {
        Member member = memberOutPutPort.loadMemberByIdName(idName);
        member.savePoint(point);
        return memberOutPutPort.saveMember(member);
    }
}
