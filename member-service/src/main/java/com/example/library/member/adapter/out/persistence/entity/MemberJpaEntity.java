package com.example.library.member.adapter.out.persistence.entity;

import com.example.library.member.domain.model.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
public class MemberJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberNo;

    @Column(nullable = false, unique = true)
    private String memberId;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_authorities", joinColumns = @JoinColumn(name = "member_no"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private List<UserRole> roles = new ArrayList<>();

    @Column(nullable = false)
    private long point;

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(
        Long memberNo,
        String memberId,
        String memberName,
        String password,
        String email,
        List<UserRole> roles,
        long point
    ) {
        this.memberNo = memberNo;
        this.memberId = memberId;
        this.memberName = memberName;
        this.password = password;
        this.email = email;
        this.roles = new ArrayList<>(roles);
        this.point = point;
    }

    public Long getMemberNo() {
        return memberNo;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public long getPoint() {
        return point;
    }
}
