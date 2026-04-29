package com.example.library.member.domain.model;

import com.example.library.common.vo.IDName;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberNo;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "member_id", unique = true)),
        @AttributeOverride(name = "name", column = @Column(name = "member_name"))
    })
    private IDName idName;

    @Embedded
    private PassWord password;

    @Embedded
    private Email email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_authority", joinColumns = @JoinColumn(name = "member_no"))
    private List<Authority> authorites = new ArrayList<>();

    @Embedded
    private Point point;

    public Member() {
    }

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

    public void setMemberNo(Long memberNo) {
        this.memberNo = memberNo;
    }

    public IDName getIdName() {
        return idName;
    }

    public void setIdName(IDName idName) {
        this.idName = idName;
    }

    public PassWord getPassword() {
        return password;
    }

    public void setPassword(PassWord password) {
        this.password = password;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public List<Authority> getAuthorites() {
        return authorites;
    }

    public void setAuthorites(List<Authority> authorites) {
        this.authorites = authorites;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }
}
