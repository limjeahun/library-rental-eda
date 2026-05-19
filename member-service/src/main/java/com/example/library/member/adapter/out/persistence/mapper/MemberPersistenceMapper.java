package com.example.library.member.adapter.out.persistence.mapper;

import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.adapter.out.persistence.entity.MemberJpaEntity;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;
import com.example.library.member.domain.vo.Point;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 회원 도메인 모델과 JPA 엔티티 사이의 변환을 담당합니다.
 */
@Component
public class MemberPersistenceMapper {
    /**
     * 회원 도메인 모델을 JPA 저장용 엔티티로 변환.
     *
     * @param member 저장하거나 응답으로 변환할 회원 도메인 모델.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환.
     */
    public MemberJpaEntity toJpaEntity(Member member) {
        List<UserRole> roles = member.authorities().stream()
            .map(Authority::role)
            .toList();

        return new MemberJpaEntity(
            member.memberNo(),
            member.idName().id(),
            member.idName().name(),
            member.password().value(),
            member.email().value(),
            roles,
            member.point().point()
        );
    }

    /**
     * JPA 엔티티를 회원 도메인 모델로 복원.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티.
     * @return 영속성 모델에서 복원한 도메인 모델을 반환.
     */
    public Member toDomain(MemberJpaEntity entity) {
        List<Authority> authorities = entity.getRoles().stream()
            .map(Authority::create)
            .toList();

        return Member.reconstitute(
            entity.getMemberNo(),
            new MemberIdentity(entity.getMemberId(), entity.getMemberName()),
            new PassWord(entity.getPassword()),
            new Email(entity.getEmail()),
            authorities,
            new Point(entity.getPoint())
        );
    }
}
