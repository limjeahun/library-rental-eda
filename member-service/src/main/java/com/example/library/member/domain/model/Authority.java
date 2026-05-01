package com.example.library.member.domain.model;

/**
 * 회원에게 부여된 사용자 권한을 표현하는 도메인 값입니다.
 */
public class Authority {
    private UserRole role;

    /**
     * 지정한 역할을 가진 권한 값을 생성합니다.
     *
     * @param role 회원에게 부여할 사용자 역할입니다.
     */
    public Authority(UserRole role) {
        this.role = role;
    }

    /**
     * 역할 값으로 회원 권한을 생성합니다.
     *
     * @param role 회원에게 부여할 사용자 역할입니다.
     * @return 지정한 사용자 역할을 감싼 권한 값 객체를 반환합니다.
     */
    public static Authority create(UserRole role) {
        return new Authority(role);
    }

    /**
     * 권한의 역할 값을 반환합니다.
     *
     * @return 회원에게 부여된 사용자 역할을 반환합니다.
     */
    public UserRole getRole() {
        return role;
    }
}
