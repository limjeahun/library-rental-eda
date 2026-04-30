package com.example.library.rental.adapter.in.messaging.consumer;

import com.example.library.common.event.EventResult;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RentalEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(RentalEventConsumer.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final HandleRentalResultUseCase handleRentalResultUseCase;

    public RentalEventConsumer(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, HandleRentalResultUseCase handleRentalResultUseCase) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.handleRentalResultUseCase = handleRentalResultUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentalResult(ConsumerRecord<String, String> record) throws Exception {
        EventResult result = objectMapper.readValue(record.value(), EventResult.class);
        if (!markProcessed(result.getEventId())) {
            log.info("skip already processed rental_result eventId={}", result.getEventId());
            return;
        }
        handleRentalResultUseCase.handle(result);
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:rental:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
