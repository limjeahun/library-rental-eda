package com.example.library.bestbook.adapter.in.messaging.consumer;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.common.event.ItemRented;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BestBookEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(BestBookEventConsumer.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final RecordBestBookRentUseCase recordBestBookRentUseCase;

    public BestBookEventConsumer(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        RecordBestBookRentUseCase recordBestBookRentUseCase
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.recordBestBookRentUseCase = recordBestBookRentUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed bestbook eventId={}", event.getEventId());
            return;
        }
        recordBestBookRentUseCase.recordRent(new RecordBestBookRentCommand(
            event.getItem().getNo(),
            event.getItem().getTitle()
        ));
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:bestbook:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
