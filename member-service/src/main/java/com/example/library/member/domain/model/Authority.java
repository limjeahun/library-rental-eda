package com.example.library.member.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Authority {
    @Enumerated(EnumType.STRING)
    private UserRole role;

    public Authority() {
    }

    public Authority(UserRole role) {
        this.role = role;
    }

    public static Authority create(UserRole role) {
        return new Authority(role);
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
