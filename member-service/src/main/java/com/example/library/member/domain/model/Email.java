package com.example.library.member.domain.model;

public class Email {
    private String value;

    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
