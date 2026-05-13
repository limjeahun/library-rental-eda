package com.example.library.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * book-service Kafka topic 설정입니다.
 */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record BookKafkaTopicProperties(String rentalResult) {
}
