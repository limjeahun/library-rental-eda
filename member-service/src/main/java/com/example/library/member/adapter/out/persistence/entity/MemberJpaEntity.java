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
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 회원 도메인 모델을 MariaDB에 저장하기 위한 JPA 엔티티입니다.
 */
@Getter
@Entity
@Table(name = "members")
public class MemberJpaEntity {
    /**
     *  회원 번호를 반환.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberNo;

    /**
     *  회원 로그인 ID를 반환.
     */
    @Column(nullable = false, unique = true)
    private String memberId;

    /**
     *  회원 이름을 반환.
     */
    @Column(nullable = false)
    private String memberName;

    /**
     *  저장된 비밀번호 값을 반환.
     */
    @Column(nullable = false)
    private String password;

    /**
     *  회원 이메일을 반환.
     */
    @Column(nullable = false)
    private String email;

    /**
     *  회원 권한 목록을 반환.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_authorities", joinColumns = @JoinColumn(name = "member_no"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private List<UserRole> roles = new ArrayList<>();

    /**
     *  회원 보유 포인트를 반환.
     */
    @Column(nullable = false)
    private long point;

    /**
     * JPA 가 엔티티를 생성할 때 사용하는 기본 생성자입니다.
     */
    protected MemberJpaEntity() {
    }

    /**
     * 저장소 어댑터가 도메인 모델의 현재 상태를 JPA 엔티티로 옮길 때 사용합니다.
     *
     * @param memberNo 조회할 회원 번호입니다.
     * @param memberId 조회할 회원 로그인 ID입니다.
     * @param memberName 저장할 회원 이름입니다.
     * @param password 저장하거나 검증할 비밀번호 값입니다.
     * @param email 저장하거나 검증할 이메일 값입니다.
     * @param roles 회원에게 부여된 권한 목록입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
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

}
