package com.example.library.member.domain.model;

import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;
import com.example.library.member.domain.vo.Point;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 회원 식별 정보, 권한, 포인트를 관리하는 순수 도메인 모델.
 */
@Getter
public class Member {
    /**
     *  회원 번호를 반환합니다.
     */
    private final Long memberNo;
    /**
     *  회원 ID와 이름 값을 반환합니다.
     */
    private final MemberIdentity idName;
    /**
     *  회원 비밀번호 값을 반환합니다.
     */
    private final PassWord password;
    /**
     *  회원 이메일 값을 반환합니다.
     */
    private final Email email;

    /**
     *  회원 권한 목록.
     */
    private List<Authority> authorities = new ArrayList<>();
    /**
     *  회원 포인트 값 객체를 반환합니다.
     */
    private Point point;

    /**
     * 영속성 어댑터가 저장된 회원 상태를 도메인 모델로 복원할 때 사용.
     *
     * @param memberNo 조회할 회원 번호.
     * @param idName 대상 회원의 ID와 이름을 담은 값 객체.
     * @param password 저장하거나 검증할 비밀번호 값.
     * @param email 저장하거나 검증할 이메일 값.
     * @param authorities 회원에게 부여된 권한 목록.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     */
    public Member(
            Long            memberNo,
            MemberIdentity  idName,
            PassWord        password,
            Email           email,
            List<Authority> authorities,
            Point           point
    ) {
        this.memberNo    = memberNo;
        this.idName      = idName;
        this.password    = password;
        this.email       = email;
        this.authorities = authorities;
        this.point       = point;
    }

    /**
     * 기본 USER 권한과 0 포인트를 가진 새 회원을 등록.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 값 객체.
     * @param pwd 회원 등록에 사용할 비밀번호 값 객체.
     * @param email 저장하거나 검증할 이메일 값.
     * @return 기본 USER 권한과 0포인트로 초기화된 회원 도메인 모델을 반환.
     */
    public static Member registerMember(MemberIdentity idName, PassWord pwd, Email email) {
        Member member = new Member(null, idName, pwd, email, new ArrayList<>(), new Point(0));
        member.addAuthority(Authority.create(UserRole.USER));
        return member;
    }

    /**
     * 보유 포인트에 지정한 포인트를 적립.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     * @return 적립 후 회원의 보유 포인트 잔액을 반환.
     */
    public long savePoint(long point) {
        this.point = this.point.savePoint(point);
        return this.point.point();
    }

    /**
     * 보유 포인트에서 지정한 포인트를 사용.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     * @return 차감 후 회원의 보유 포인트 잔액을 반환.
     */
    public long usePoint(long point) {
        this.point = this.point.usePoint(point);
        return this.point.point();
    }

    /**
     * 회원 권한을 추가합니다.
     *
     * @param authority 회원에게 부여할 권한 역할입니다.
     */
    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
    }

    /**
     * 회원 권한 목록을 반환합니다.
     *
     * @return 회원에게 부여된 권한 값 목록을 반환합니다.
     */
    public List<Authority> getAuthorities() {
        return List.copyOf(authorities);
    }

}
