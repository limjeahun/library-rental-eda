package com.example.library.book.adapter.in.messaging.consumer;

import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(BookEventConsumer.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final HandleBookRentalEventUseCase handleBookRentalEventUseCase;

    public BookEventConsumer(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        HandleBookRentalEventUseCase handleBookRentalEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.handleBookRentalEventUseCase = handleBookRentalEventUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book rent eventId={}", event.getEventId());
            return;
        }
        handleBookRentalEventUseCase.handleRent(event);
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ConsumerRecord<String, String> record) throws Exception {
        ItemReturned event = objectMapper.readValue(record.value(), ItemReturned.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book return eventId={}", event.getEventId());
            return;
        }
        handleBookRentalEventUseCase.handleReturn(event);
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:book:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
