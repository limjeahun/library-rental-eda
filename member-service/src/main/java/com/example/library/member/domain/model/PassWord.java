package com.example.library.member.domain.model;

public class PassWord {
    private String value;

    public PassWord(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
