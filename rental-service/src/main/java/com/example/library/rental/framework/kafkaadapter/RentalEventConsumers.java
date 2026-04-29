package com.example.library.rental.framework.kafkaadapter;

import com.example.library.common.event.EventResult;
import com.example.library.rental.application.usecase.CompensationUsecase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RentalEventConsumers {
    private static final Logger log = LoggerFactory.getLogger(RentalEventConsumers.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final CompensationUsecase compensationUsecase;

    public RentalEventConsumers(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, CompensationUsecase compensationUsecase) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.compensationUsecase = compensationUsecase;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentalResult(ConsumerRecord<String, String> record) throws Exception {
        EventResult result = objectMapper.readValue(record.value(), EventResult.class);
        if (!markProcessed(result.getEventId())) {
            log.info("skip already processed rental_result eventId={}", result.getEventId());
            return;
        }
        if (result.isSuccessed()) {
            log.info("SAGA participant success eventType={} eventId={}", result.getEventType(), result.getEventId());
            return;
        }
        switch (result.getEventType()) {
            case RENT -> {
                log.info("대여취소 보상트랜젝션 실행");
                compensationUsecase.cancleRentItem(result.getIdName(), result.getItem());
            }
            case RETURN -> {
                log.info("반납취소 보상트랜젝션 실행");
                compensationUsecase.cancleReturnItem(result.getIdName(), result.getItem(), result.getPoint());
            }
            case OVERDUE -> {
                log.info("연체해제처리취소 보상트랜젝션 실행");
                compensationUsecase.cancleMakeAvailableRental(result.getIdName(), result.getPoint());
            }
            default -> log.warn("unsupported eventType={}", result.getEventType());
        }
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:rental:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
