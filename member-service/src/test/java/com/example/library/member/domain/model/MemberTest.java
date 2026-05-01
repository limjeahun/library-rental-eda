package com.example.library.member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.vo.IDName;
import org.junit.jupiter.api.Test;

/**
 * 회원 도메인 모델의 등록과 포인트 변경 규칙을 검증합니다.
 */
class MemberTest {
    /**
     * 회원 등록 시 기본 권한과 초기 포인트가 설정되는지 검증합니다. @Test void registerMember() { Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com")); assertThat(member.getIdName().getId()).isEqualTo("jenny"); assertThat(member.getAuthorites()).hasSize(1); assertThat(member.getPoint().getPoint()).isZero(); } 포인트 적립 시 보유 포인트가 증가하는지 검증합니다. @Test void savePoint() { Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com")); member.savePoint(10); assertThat(member.getPoint().getPoint()).isEqualTo(10); } 포인트 사용 시 보유 포인트가 차감되는지 검증합니다. @Test void usePoint() { Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com")); member.savePoint(20); member.usePoint(10); assertThat(member.getPoint().getPoint()).isEqualTo(10); } 보유 포인트보다 많은 포인트 사용을 거부하는지 검증합니다. @Test void usePointMoreThanOwnedThrowsException() { Member member = Member.registerMember(idName(), new PassWord("1111"), new Email("a@b.com")); assertThatThrownBy(() -> member.usePoint(10)) .isInstanceOf(IllegalArgumentException.class); } 테스트용 회원 식별 값을 생성합니다.
     *
     * @return 테스트 회원의 ID와 이름을 담은 값 객체를 반환합니다.
     */
    private IDName idName() {
        return new IDName("jenny", "제니");
    }
}
