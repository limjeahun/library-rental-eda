package com.example.library.member.framework.kafkaadapter;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.member.application.usecase.SavePointUsecase;
import com.example.library.member.application.usecase.UsePointUsecase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MemberEventConsumers {
    private static final Logger log = LoggerFactory.getLogger(MemberEventConsumers.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final SavePointUsecase savePointUsecase;
    private final UsePointUsecase usePointUsecase;
    private final MemberEventProducer memberEventProducer;
    private final boolean forceOverdueClearFail;

    public MemberEventConsumers(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        SavePointUsecase savePointUsecase,
        UsePointUsecase usePointUsecase,
        MemberEventProducer memberEventProducer,
        @Value("${app.failure.force-overdue-clear-fail:false}") boolean forceOverdueClearFail
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.savePointUsecase = savePointUsecase;
        this.usePointUsecase = usePointUsecase;
        this.memberEventProducer = memberEventProducer;
        this.forceOverdueClearFail = forceOverdueClearFail;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed member rent eventId={}", event.getEventId());
            return;
        }
        try {
            savePointUsecase.savePoint(event.getIdName(), event.getPoint());
        } catch (Exception ex) {
            log.error("member rent point save failed eventId={}", event.getEventId(), ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ConsumerRecord<String, String> record) throws Exception {
        ItemReturned event = objectMapper.readValue(record.value(), ItemReturned.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed member return eventId={}", event.getEventId());
            return;
        }
        try {
            savePointUsecase.savePoint(event.getIdName(), event.getPoint());
        } catch (Exception ex) {
            log.error("member return point save failed eventId={}", event.getEventId(), ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.overdue-clear}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeClear(ConsumerRecord<String, String> record) throws Exception {
        OverdueCleared event = objectMapper.readValue(record.value(), OverdueCleared.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed overdue clear eventId={}", event.getEventId());
            return;
        }
        EventResult result = processClear(event);
        memberEventProducer.publish(result);
    }

    @KafkaListener(topics = "${app.kafka.topics.point-use}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUsePoint(ConsumerRecord<String, String> record) throws Exception {
        PointUseCommand command = objectMapper.readValue(record.value(), PointUseCommand.class);
        if (!markProcessed(command.getEventId())) {
            log.info("skip already processed point_use eventId={}", command.getEventId());
            return;
        }
        try {
            usePointUsecase.userPoint(command.getIdName(), command.getPoint());
        } catch (Exception ex) {
            log.error("point_use command failed eventId={} reason={}", command.getEventId(), command.getReason(), ex);
        }
    }

    private EventResult processClear(OverdueCleared event) {
        try {
            if (forceOverdueClearFail) {
                throw new IllegalArgumentException("forced overdue_clear failure");
            }
            usePointUsecase.userPoint(event.getIdName(), event.getPoint());
            return result(event, true, null);
        } catch (Exception ex) {
            log.error("overdue clear failed eventId={}", event.getEventId(), ex);
            return result(event, false, ex.getMessage());
        }
    }

    private EventResult result(OverdueCleared event, boolean successed, String reason) {
        return new EventResult(
            event.getEventId(),
            event.getCorrelationId(),
            Instant.now(),
            EventType.OVERDUE,
            successed,
            event.getIdName(),
            null,
            event.getPoint(),
            reason
        );
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:member:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
