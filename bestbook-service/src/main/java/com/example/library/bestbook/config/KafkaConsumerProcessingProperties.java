package com.example.library.bestbook.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * bestbook-service Kafka consumer processing lock 설정입니다.
 */
@ConfigurationProperties(prefix = "app.kafka.consumer.processing")
public record KafkaConsumerProcessingProperties(Duration ttl) {
}
