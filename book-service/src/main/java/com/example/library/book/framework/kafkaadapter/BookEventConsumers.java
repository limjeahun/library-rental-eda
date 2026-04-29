package com.example.library.book.framework.kafkaadapter;

import com.example.library.book.application.usecase.MakeAvailableUsecase;
import com.example.library.book.application.usecase.MakeUnAvailableUsecase;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
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
public class BookEventConsumers {
    private static final Logger log = LoggerFactory.getLogger(BookEventConsumers.class);
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final MakeAvailableUsecase makeAvailableUsecase;
    private final MakeUnAvailableUsecase makeUnAvailableUsecase;
    private final BookEventProducer bookEventProducer;
    private final boolean forceRentFail;
    private final boolean forceReturnFail;

    public BookEventConsumers(
        ObjectMapper objectMapper,
        StringRedisTemplate redisTemplate,
        MakeAvailableUsecase makeAvailableUsecase,
        MakeUnAvailableUsecase makeUnAvailableUsecase,
        BookEventProducer bookEventProducer,
        @Value("${app.failure.force-rent-fail:false}") boolean forceRentFail,
        @Value("${app.failure.force-return-fail:false}") boolean forceReturnFail
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.makeAvailableUsecase = makeAvailableUsecase;
        this.makeUnAvailableUsecase = makeUnAvailableUsecase;
        this.bookEventProducer = bookEventProducer;
        this.forceRentFail = forceRentFail;
        this.forceReturnFail = forceReturnFail;
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book rent eventId={}", event.getEventId());
            return;
        }
        EventResult result = processRent(event);
        bookEventProducer.publish(result);
    }

    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ConsumerRecord<String, String> record) throws Exception {
        ItemReturned event = objectMapper.readValue(record.value(), ItemReturned.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book return eventId={}", event.getEventId());
            return;
        }
        EventResult result = processReturn(event);
        bookEventProducer.publish(result);
    }

    private EventResult processRent(ItemRented event) {
        try {
            if (forceRentFail) {
                throw new IllegalArgumentException("forced rental_rent failure");
            }
            makeUnAvailableUsecase.makeUnAvailable(event.getItem().getNo());
            return result(event, EventType.RENT, true, null);
        } catch (Exception ex) {
            log.error("Book rent event failed eventId={}", event.getEventId(), ex);
            return result(event, EventType.RENT, false, ex.getMessage());
        }
    }

    private EventResult processReturn(ItemReturned event) {
        try {
            if (forceReturnFail) {
                throw new IllegalArgumentException("forced rental_return failure");
            }
            makeAvailableUsecase.makeAvailable(event.getItem().getNo());
            return result(event, EventType.RETURN, true, null);
        } catch (Exception ex) {
            log.error("Book return event failed eventId={}", event.getEventId(), ex);
            return result(event, EventType.RETURN, false, ex.getMessage());
        }
    }

    private EventResult result(ItemRented event, EventType eventType, boolean successed, String reason) {
        return new EventResult(
            event.getEventId(),
            event.getCorrelationId(),
            Instant.now(),
            eventType,
            successed,
            event.getIdName(),
            event.getItem(),
            event.getPoint(),
            reason
        );
    }

    private boolean markProcessed(String eventId) {
        String key = "processed:book:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
