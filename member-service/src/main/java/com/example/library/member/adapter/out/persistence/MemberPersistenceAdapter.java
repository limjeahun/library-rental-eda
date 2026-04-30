package com.example.library.member.adapter.out.persistence;

import com.example.library.common.vo.IDName;
import com.example.library.member.adapter.out.persistence.mapper.MemberPersistenceMapper;
import com.example.library.member.adapter.out.persistence.repository.MemberJpaRepository;
import com.example.library.member.application.port.out.MemberOutputPort;
import com.example.library.member.domain.model.Member;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Repository;

@Repository
public class MemberPersistenceAdapter implements MemberOutputPort {
    private final MemberJpaRepository repository;
    private final MemberPersistenceMapper mapper;

    public MemberPersistenceAdapter(MemberJpaRepository repository, MemberPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Member saveMember(Member member) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(member)));
    }

    @Override
    public Member loadMember(long memberNo) {
        return repository.findById(memberNo)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + memberNo));
    }

    @Override
    public Member loadMemberByIdName(IDName idName) {
        return repository.findByMemberId(idName.getId())
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + idName.getId()));
    }
}
