package com.example.library.member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.member.domain.event.MemberDomainEvent;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;
import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;
import com.example.library.member.domain.vo.Point;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberTest {
    @Test
    void registerMember() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        assertThat(member.getIdName().id()).isEqualTo("jenny");
        assertThat(member.getAuthorities()).hasSize(1);
        assertThat(member.getPoint().point()).isZero();
    }

    @Test
    void savePoint() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        member.savePoint(10);

        assertThat(member.getPoint().point()).isEqualTo(10);
        MemberPointSavedDomainEvent event = pullSingleEvent(member, MemberPointSavedDomainEvent.class);
        assertThat(event.member()).isEqualTo(idName());
        assertThat(event.point()).isEqualTo(10);
        assertThat(member.pullDomainEvents()).isEmpty();
    }

    @Test
    void usePoint() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));
        member.savePoint(20);
        member.pullDomainEvents();

        member.usePoint(10);

        assertThat(member.getPoint().point()).isEqualTo(10);
        MemberPointUsedDomainEvent event = pullSingleEvent(member, MemberPointUsedDomainEvent.class);
        assertThat(event.member()).isEqualTo(idName());
        assertThat(event.point()).isEqualTo(10);
        assertThat(member.pullDomainEvents()).isEmpty();
    }

    @Test
    void usePointMoreThanOwnedThrowsException() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        assertThatThrownBy(() -> member.usePoint(10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authoritiesAreNotExternallyMutable() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        assertThatThrownBy(() -> member.getAuthorities().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reconstitutedMemberStartsWithoutDomainEvents() {
        Member member = Member.reconstitute(
            1L,
            idName(),
            new PassWord("1111"),
            new Email("a@b.com"),
            List.of(Authority.create(UserRole.USER)),
            new Point(10)
        );

        assertThat(member.pullDomainEvents()).isEmpty();
    }

    private <T extends MemberDomainEvent> T pullSingleEvent(Member member, Class<T> eventType) {
        List<MemberDomainEvent> events = member.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(eventType);
        return eventType.cast(events.get(0));
    }

    private MemberIdentity idName() {
        return new MemberIdentity("jenny", "제니");
    }
}
