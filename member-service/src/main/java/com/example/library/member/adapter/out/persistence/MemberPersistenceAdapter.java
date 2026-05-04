package com.example.library.member.adapter.out.persistence;

import com.example.library.common.vo.IDName;
import com.example.library.member.adapter.out.persistence.mapper.MemberPersistenceMapper;
import com.example.library.member.adapter.out.persistence.repository.MemberJpaRepository;
import com.example.library.member.application.port.out.LoadMemberByIdNamePort;
import com.example.library.member.application.port.out.LoadMemberPort;
import com.example.library.member.application.port.out.SaveMemberPort;
import com.example.library.member.domain.model.Member;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 회원 도메인 모델을 JPA 엔티티로 저장하고 회원 번호 또는 회원 ID로 다시 도메인 모델을 복원하는 영속성 컴포넌트입니다.
 */
@Repository
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements LoadMemberByIdNamePort, LoadMemberPort, SaveMemberPort {
    private final MemberJpaRepository repository;
    private final MemberPersistenceMapper mapper;

    /**
     * 회원 도메인 모델을 JPA 엔티티로 변환해 저장한 뒤 다시 도메인 모델로 반환합니다.
     *
     * @param member 저장하거나 응답으로 변환할 회원 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 포인트가 반영된 회원 도메인 모델을 반환합니다.
     */
    @Override
    public Member saveMember(Member member) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(member)));
    }

    /**
     * 회원 번호로 JPA 엔티티를 조회하고 도메인 모델로 변환합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @return 회원 번호에 해당하는 회원 도메인 모델을 반환합니다.
     */
    @Override
    public Member loadMember(long memberNo) {
        return repository.findById(memberNo)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + memberNo));
    }

    /**
     * 회원 ID로 JPA 엔티티를 조회하고 도메인 모델로 변환합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @return 회원 ID에 해당하는 회원 도메인 모델을 반환합니다.
     */
    @Override
    public Member loadMemberByIdName(IDName idName) {
        return repository.findByMemberId(idName.id())
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + idName.id()));
    }
}
