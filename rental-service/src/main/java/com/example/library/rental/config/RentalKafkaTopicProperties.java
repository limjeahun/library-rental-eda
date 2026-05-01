package com.example.library.rental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record RentalKafkaTopicProperties(
    String rentalRent,
    String rentalReturn,
    String overdueClear,
    String pointUse
) {
}
