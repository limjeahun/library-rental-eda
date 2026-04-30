package com.example.library.member.adapter.out.persistence.mapper;

import com.example.library.common.vo.IDName;
import com.example.library.member.adapter.out.persistence.entity.MemberJpaEntity;
import com.example.library.member.domain.model.Authority;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.PassWord;
import com.example.library.member.domain.model.Point;
import com.example.library.member.domain.model.UserRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MemberPersistenceMapper {
    public MemberJpaEntity toJpaEntity(Member member) {
        List<UserRole> roles = member.getAuthorites().stream()
            .map(Authority::getRole)
            .toList();

        return new MemberJpaEntity(
            member.getMemberNo(),
            member.getIdName().getId(),
            member.getIdName().getName(),
            member.getPassword().getValue(),
            member.getEmail().getValue(),
            roles,
            member.getPoint().getPoint()
        );
    }

    public Member toDomain(MemberJpaEntity entity) {
        List<Authority> authorities = entity.getRoles().stream()
            .map(Authority::create)
            .toList();

        return new Member(
            entity.getMemberNo(),
            new IDName(entity.getMemberId(), entity.getMemberName()),
            new PassWord(entity.getPassword()),
            new Email(entity.getEmail()),
            authorities,
            new Point(entity.getPoint())
        );
    }
}
