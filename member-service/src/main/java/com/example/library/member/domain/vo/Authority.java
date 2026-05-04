package com.example.library.member.domain.vo;

import com.example.library.member.domain.model.UserRole;

/**
 * 회원에게 부여된 권한 역할을 표현하는 값 객체입니다.
 */
public record Authority(UserRole role) {
    public static Authority create(UserRole role) {
        return new Authority(role);
    }
}
