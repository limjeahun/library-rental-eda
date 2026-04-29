package com.example.library.member.framework.jpaadapter;

import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MemberQueryRepository {
    private final JPAQueryFactory queryFactory;

    public MemberQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<Member> findByIdNameId(String id) {
        QMember member = QMember.member;
        return Optional.ofNullable(
            queryFactory.selectFrom(member)
                .where(member.idName.id.eq(id))
                .fetchFirst()
        );
    }
}
