package com.example.library.member.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.outputport.MemberOutPutPort;
import com.example.library.member.application.usecase.AddMemberUsecase;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.PassWord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddMemberInputPort implements AddMemberUsecase {
    private final MemberOutPutPort memberOutPutPort;

    public AddMemberInputPort(MemberOutPutPort memberOutPutPort) {
        this.memberOutPutPort = memberOutPutPort;
    }

    @Override
    public Member addMember(IDName idName, PassWord passWord, Email email) {
        return memberOutPutPort.saveMember(Member.registerMember(idName, passWord, email));
    }
}
