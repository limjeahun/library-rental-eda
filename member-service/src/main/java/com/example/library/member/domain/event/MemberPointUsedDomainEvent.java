package com.example.library.member.domain.event;

import com.example.library.member.domain.vo.MemberIdentity;
import java.time.Instant;

/**
 * 회원 포인트가 사용되었음을 표현하는 service-local domain event.
 */
public record MemberPointUsedDomainEvent(
    Instant occurredAt,
    MemberIdentity member,
    long point
) implements MemberDomainEvent {
    public static MemberPointUsedDomainEvent of(MemberIdentity member, long point) {
        return new MemberPointUsedDomainEvent(
            Instant.now(),
            member,
            point
        );
    }
}
