package com.example.library.member.framework.jpaadapter;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.outputport.MemberOutPutPort;
import com.example.library.member.domain.model.Member;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Repository;

@Repository
public class MemberJpaAdapter implements MemberOutPutPort {
    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;

    public MemberJpaAdapter(MemberRepository memberRepository, MemberQueryRepository memberQueryRepository) {
        this.memberRepository = memberRepository;
        this.memberQueryRepository = memberQueryRepository;
    }

    @Override
    public Member loadMember(long memberNo) {
        return memberRepository.findById(memberNo)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
    }

    @Override
    public Member loadMemberByIdName(IDName idName) {
        return memberQueryRepository.findByIdNameId(idName.getId())
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
    }

    @Override
    public Member saveMember(Member member) {
        return memberRepository.save(member);
    }
}
