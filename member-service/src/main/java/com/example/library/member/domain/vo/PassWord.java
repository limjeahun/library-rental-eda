package com.example.library.member.domain.vo;

/**
 * 회원 비밀번호를 표현하는 값 객체입니다.
 */
public record PassWord(String value) {
    public PassWord {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
    }
}
