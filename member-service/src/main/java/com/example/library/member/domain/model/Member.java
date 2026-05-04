package com.example.library.member.domain.model;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;
import com.example.library.member.domain.vo.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 회원 식별 정보, 권한, 포인트를 관리하는 순수 도메인 모델입니다.
 */
public class Member {
    private Long memberNo;
    private IDName idName;
    private PassWord password;
    private Email email;
    private List<Authority> authorites = new ArrayList<>();
    private Point point;

    /**
     * 영속성 어댑터가 저장된 회원 상태를 도메인 모델로 복원할 때 사용합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param password 저장하거나 검증할 비밀번호 값입니다.
     * @param email 저장하거나 검증할 이메일 값입니다.
     * @param authorites 회원에게 부여된 권한 목록입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public Member(Long memberNo, IDName idName, PassWord password, Email email, List<Authority> authorites, Point point) {
        this.memberNo = memberNo;
        this.idName = idName;
        this.password = password;
        this.email = email;
        this.authorites = authorites;
        this.point = point;
    }

    /**
     * 기본 USER 권한과 0 포인트를 가진 새 회원을 등록합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param pwd 회원 등록에 사용할 비밀번호 값 객체입니다.
     * @param email 저장하거나 검증할 이메일 값입니다.
     * @return 기본 USER 권한과 0포인트로 초기화된 회원 도메인 모델을 반환합니다.
     */
    public static Member registerMember(IDName idName, PassWord pwd, Email email) {
        Member member = new Member(null, idName, pwd, email, new ArrayList<>(), new Point(0));
        member.addAuthority(Authority.create(UserRole.USER));
        return member;
    }

    /**
     * 보유 포인트에 지정한 포인트를 적립합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 적립 후 회원의 보유 포인트 잔액을 반환합니다.
     */
    public long savePoint(long point) {
        this.point = this.point.savePoint(point);
        return this.point.point();
    }

    /**
     * 보유 포인트에서 지정한 포인트를 사용합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 차감 후 회원의 보유 포인트 잔액을 반환합니다.
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
        this.authorites.add(authority);
    }

    /**
     * 회원 번호를 반환합니다.
     *
     * @return 회원 저장소에서 부여한 회원 번호를 반환합니다.
     */
    public Long getMemberNo() {
        return memberNo;
    }

    /**
     * 회원 ID와 이름 값을 반환합니다.
     *
     * @return 메시지 대상 회원의 ID와 이름을 반환합니다.
     */
    public IDName getIdName() {
        return idName;
    }

    /**
     * 회원 비밀번호 값을 반환합니다.
     *
     * @return 회원 비밀번호 값 객체를 반환합니다.
     */
    public PassWord getPassword() {
        return password;
    }

    /**
     * 회원 이메일 값을 반환합니다.
     *
     * @return 회원 이메일 값 객체를 반환합니다.
     */
    public Email getEmail() {
        return email;
    }

    /**
     * 회원 권한 목록을 반환합니다.
     *
     * @return 회원에게 부여된 권한 값 목록을 반환합니다.
     */
    public List<Authority> getAuthorites() {
        return Collections.unmodifiableList(authorites);
    }

    /**
     * 회원 포인트 값 객체를 반환합니다.
     *
     * @return 현재 저장된 포인트 금액을 반환합니다.
     */
    public Point getPoint() {
        return point;
    }
}
