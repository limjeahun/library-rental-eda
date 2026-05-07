package com.example.library.book.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * book-service Kafka consumer processing lock 설정입니다.
 */
@ConfigurationProperties(prefix = "app.kafka.consumer.processing")
public record KafkaConsumerProcessingProperties(Duration ttl) {
}
