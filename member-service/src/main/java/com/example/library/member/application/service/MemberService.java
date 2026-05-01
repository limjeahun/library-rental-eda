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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 등록, 조회, 포인트 적립/사용 흐름을 조율하는 application service입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements AddMemberUseCase, MemberQueryUseCase, SavePointUseCase, UsePointUseCase {
    private final MemberOutputPort memberOutputPort;

    /**
     * 회원 등록 command를 도메인 모델로 변환하고 저장 결과를 반환합니다.
     *
     * @param command 등록할 회원 ID, 이름, 이메일, 비밀번호를 담은 application command입니다.
     * @return 등록된 회원의 번호, ID, 이름, 이메일, 포인트를 담은 결과 DTO를 반환합니다.
     */
    @Override
    public MemberResult addMember(AddMemberCommand command) {
        Member member = Member.registerMember(command.idName(), command.passWord(), command.email());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }

    /**
     * 회원 번호로 회원을 조회합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @return 회원 번호에 해당하는 회원 결과 DTO를 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public MemberResult getMember(long memberNo) {
        return MemberResult.from(memberOutputPort.loadMember(memberNo));
    }

    /**
     * 회원 로그인 ID로 회원을 조회합니다.
     *
     * @param id 조회하거나 포인트를 변경할 회원 ID입니다.
     * @return 회원 ID에 해당하는 회원 결과 DTO를 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public MemberResult getMemberById(String id) {
        return MemberResult.from(memberOutputPort.loadMemberByIdName(new IDName(id, null)));
    }

    /**
     * 회원 포인트를 적립하고 저장 결과를 반환합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     * @return 적립 후 보유 포인트 잔액을 반환합니다.
     */
    @Override
    public MemberResult savePoint(ChangePointCommand command) {
        Member member = memberOutputPort.loadMemberByIdName(command.idName());
        member.savePoint(command.point());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }

    /**
     * 회원 포인트를 사용하고 저장 결과를 반환합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     * @return 차감 후 보유 포인트 잔액을 반환합니다.
     */
    @Override
    public MemberResult usePoint(ChangePointCommand command) {
        Member member = memberOutputPort.loadMemberByIdName(command.idName());
        member.usePoint(command.point());
        return MemberResult.from(memberOutputPort.saveMember(member));
    }
}
