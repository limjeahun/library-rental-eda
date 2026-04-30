package com.example.library.member.domain.model;

import com.example.library.common.vo.IDName;
import java.util.ArrayList;
import java.util.List;

public class Member {
    private Long memberNo;
    private IDName idName;
    private PassWord password;
    private Email email;
    private List<Authority> authorites = new ArrayList<>();
    private Point point;

    public Member(Long memberNo, IDName idName, PassWord password, Email email, List<Authority> authorites, Point point) {
        this.memberNo = memberNo;
        this.idName = idName;
        this.password = password;
        this.email = email;
        this.authorites = authorites;
        this.point = point;
    }

    public static Member registerMember(IDName idName, PassWord pwd, Email email) {
        Member member = new Member(null, idName, pwd, email, new ArrayList<>(), new Point(0));
        member.addAuthority(Authority.create(UserRole.USER));
        return member;
    }

    public long savePoint(long point) {
        return this.point.savePoint(point);
    }

    public long usePoint(long point) {
        return this.point.usePoint(point);
    }

    public void addAuthority(Authority authority) {
        this.authorites.add(authority);
    }

    public Long getMemberNo() {
        return memberNo;
    }

    public IDName getIdName() {
        return idName;
    }

    public PassWord getPassword() {
        return password;
    }

    public Email getEmail() {
        return email;
    }

    public List<Authority> getAuthorites() {
        return authorites;
    }

    public Point getPoint() {
        return point;
    }
}
