package com.example.library.rental.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rental-service Kafka consumer processing lock 설정입니다.
 */
@ConfigurationProperties(prefix = "app.kafka.consumer.processing")
public record KafkaConsumerProcessingProperties(Duration ttl) {
}
