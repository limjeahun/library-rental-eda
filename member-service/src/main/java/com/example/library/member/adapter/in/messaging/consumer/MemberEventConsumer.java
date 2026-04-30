package com.example.library.member.adapter.in.messaging.consumer;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import com.example.library.member.application.port.out.MemberEventOutputPort;
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
public class MemberEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(MemberEventConsumer.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final SavePointUseCase savePointUseCase;
    private final UsePointUseCase usePointUseCase;
    private final MemberEventOutputPort memberEventOutputPort;
    private final boolean forceOverdueClearFail;

    public MemberEventConsumer(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        SavePointUseCase savePointUseCase,
        UsePointUseCase usePointUseCase,
        MemberEventOutputPort memberEventOutputPort,
        @Value("${app.failure.force-overdue-clear-fail:false}") boolean forceOverdueClearFail
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.savePointUseCase = savePointUseCase;
        this.usePointUseCase = usePointUseCase;
        this.memberEventOutputPort = memberEventOutputPort;
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
            savePointUseCase.savePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
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
            savePointUseCase.savePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
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
        memberEventOutputPort.publish(result);
    }

    @KafkaListener(topics = "${app.kafka.topics.point-use}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUsePoint(ConsumerRecord<String, String> record) throws Exception {
        PointUseCommand command = objectMapper.readValue(record.value(), PointUseCommand.class);
        if (!markProcessed(command.getEventId())) {
            log.info("skip already processed point_use eventId={}", command.getEventId());
            return;
        }
        try {
            usePointUseCase.usePoint(new ChangePointCommand(command.getIdName(), command.getPoint()));
        } catch (Exception ex) {
            log.error("point_use command failed eventId={} reason={}", command.getEventId(), command.getReason(), ex);
        }
    }

    private EventResult processClear(OverdueCleared event) {
        try {
            if (forceOverdueClearFail) {
                throw new IllegalArgumentException("forced overdue_clear failure");
            }
            usePointUseCase.usePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
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
