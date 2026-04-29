package com.example.library.member.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.outputport.MemberOutPutPort;
import com.example.library.member.application.usecase.InquiryMemberUsecase;
import com.example.library.member.domain.model.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InquiryMemberInputPort implements InquiryMemberUsecase {
    private final MemberOutPutPort memberOutPutPort;

    public InquiryMemberInputPort(MemberOutPutPort memberOutPutPort) {
        this.memberOutPutPort = memberOutPutPort;
    }

    @Override
    public Member getMember(long memberNo) {
        return memberOutPutPort.loadMember(memberNo);
    }

    @Override
    public Member getMemberById(String id) {
        return memberOutPutPort.loadMemberByIdName(new IDName(id, null));
    }
}
