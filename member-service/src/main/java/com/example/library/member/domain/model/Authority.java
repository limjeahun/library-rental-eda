package com.example.library.member.domain.model;

public class Authority {
    private UserRole role;

    public Authority(UserRole role) {
        this.role = role;
    }

    public static Authority create(UserRole role) {
        return new Authority(role);
    }

    public UserRole getRole() {
        return role;
    }
}
