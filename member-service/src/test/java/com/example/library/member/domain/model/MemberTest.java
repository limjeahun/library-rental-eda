package com.example.library.member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.vo.IDName;
import org.junit.jupiter.api.Test;

class MemberTest {
    @Test
    void registerMember() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        assertThat(member.getIdName().getId()).isEqualTo("jenny");
        assertThat(member.getAuthorites()).hasSize(1);
        assertThat(member.getPoint().getPoint()).isZero();
    }

    @Test
    void savePoint() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        member.savePoint(10);

        assertThat(member.getPoint().getPoint()).isEqualTo(10);
    }

    @Test
    void usePoint() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));
        member.savePoint(20);

        member.usePoint(10);

        assertThat(member.getPoint().getPoint()).isEqualTo(10);
    }

    @Test
    void usePointMoreThanOwnedThrowsException() {
        Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com"));

        assertThatThrownBy(() -> member.usePoint(10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private IDName idName() {
        return new IDName("jenny", "제니");
    }
}
