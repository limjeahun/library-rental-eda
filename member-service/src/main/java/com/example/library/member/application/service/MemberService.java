package com.example.library.member.application.service;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.dto.MemberResult;
import com.example.library.member.application.port.in.AddMemberUseCase;
import com.example.library.member.application.port.in.MemberQueryUseCase;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import com.example.library.member.application.port.out.MemberOutputPort;
import com.example.library.member.domain.model.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService implements AddMemberUseCase, MemberQueryUseCase, SavePointUseCase, UsePointUseCase {
    private final MemberOutputPort memberOutputPort;

    public MemberService(MemberOutputPort memberOutputPort) {
        this.memberOutputPort = memberOutputPort;
    }

    @Override
    public MemberResult addMember(AddMemberCommand command) {
        Member member = Member.registerMember(command.idName(), command.passWord(), command.email());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResult getMember(long memberNo) {
        return MemberResult.from(memberOutputPort.loadMember(memberNo));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResult getMemberById(String id) {
        return MemberResult.from(memberOutputPort.loadMemberByIdName(new IDName(id, null)));
    }

    @Override
    public MemberResult savePoint(ChangePointCommand command) {
        Member member = memberOutputPort.loadMemberByIdName(command.idName());
        member.savePoint(command.point());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }

    @Override
    public MemberResult usePoint(ChangePointCommand command) {
        Member member = memberOutputPort.loadMemberByIdName(command.idName());
        member.usePoint(command.point());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }
}
