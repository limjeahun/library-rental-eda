package com.example.library.member.domain.vo;

/**
 * 회원 이메일 주소를 표현하는 값 객체입니다.
 */
public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    }
}
