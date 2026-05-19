package com.example.library.member.domain.event;

import com.example.library.member.domain.vo.MemberIdentity;
import java.time.Instant;

/**
 * 회원 포인트가 적립되었음을 표현하는 service-local domain event.
 */
public record MemberPointSavedDomainEvent(
    Instant occurredAt,
    MemberIdentity member,
    long point
) implements MemberDomainEvent {
    public static MemberPointSavedDomainEvent of(MemberIdentity member, long point) {
        return new MemberPointSavedDomainEvent(
            Instant.now(),
            member,
            point
        );
    }
}
