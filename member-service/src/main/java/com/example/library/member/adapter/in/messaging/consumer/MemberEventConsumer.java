package com.example.library.member.adapter.in.messaging.consumer;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MemberEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(MemberEventConsumer.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final HandleMemberEventUseCase handleMemberEventUseCase;

    public MemberEventConsumer(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        HandleMemberEventUseCase handleMemberEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.handleMemberEventUseCase = handleMemberEventUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed member rent eventId={}", event.getEventId());
            return;
        }
        handleMemberEventUseCase.handleRent(event);
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ConsumerRecord<String, String> record) throws Exception {
        ItemReturned event = objectMapper.readValue(record.value(), ItemReturned.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed member return eventId={}", event.getEventId());
            return;
        }
        handleMemberEventUseCase.handleReturn(event);
    }

    @KafkaListener(topics = "${app.kafka.topics.overdue-clear}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeClear(ConsumerRecord<String, String> record) throws Exception {
        OverdueCleared event = objectMapper.readValue(record.value(), OverdueCleared.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed overdue clear eventId={}", event.getEventId());
            return;
        }
        handleMemberEventUseCase.handleOverdueClear(event);
    }

    @KafkaListener(topics = "${app.kafka.topics.point-use}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUsePoint(ConsumerRecord<String, String> record) throws Exception {
        PointUseCommand command = objectMapper.readValue(record.value(), PointUseCommand.class);
        if (!markProcessed(command.getEventId())) {
            log.info("skip already processed point_use eventId={}", command.getEventId());
            return;
        }
        handleMemberEventUseCase.handlePointUse(command);
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:member:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
