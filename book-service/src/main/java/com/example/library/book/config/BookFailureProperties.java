package com.example.library.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.failure")
public record BookFailureProperties(
    boolean forceRentFail,
    boolean forceReturnFail
) {
}
