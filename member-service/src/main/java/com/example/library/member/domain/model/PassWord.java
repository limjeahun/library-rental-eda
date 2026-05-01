package com.example.library.member.domain.model;

/**
 * 회원 비밀번호 입력 규칙을 보장하는 값 객체입니다.
 */
public class PassWord {
    private String value;

    /**
     * 비어 있지 않은 비밀번호 값을 생성합니다.
     *
     * @param value 회원 비밀번호 문자열입니다.
     */
    public PassWord(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        this.value = value;
    }

    /**
     * 비밀번호 문자열 값을 반환합니다.
     *
     * @return 검증을 통과해 저장된 회원 비밀번호 문자열을 반환합니다.
     */
    public String getValue() {
        return value;
    }
}
