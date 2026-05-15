package com.example.library.member.domain.event;

import com.example.library.member.domain.vo.MemberIdentity;

/**
 * 회원 포인트가 사용되었음을 표현하는 service-local domain event.
 */
public record MemberPointUsedDomainEvent(
    MemberIdentity member,
    long point
) implements MemberDomainEvent {
}
