package com.example.library.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.failure")
public record MemberFailureProperties(
    boolean forceOverdueClearFail
) {
}
