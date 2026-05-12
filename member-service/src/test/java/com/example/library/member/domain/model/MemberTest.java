package com.example.library.member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;
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
    }

    @Test
    void usePoint() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));
        member.savePoint(20);

        member.usePoint(10);

        assertThat(member.getPoint().point()).isEqualTo(10);
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

    private MemberIdentity idName() {
        return new MemberIdentity("jenny", "제니");
    }
}
