package com.example.library.book.adapter.in.messaging.consumer;

import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 대여 서비스가 발행한 대여/반납 이벤트를 받아 도서의 대여 가능 상태 변경과 결과 이벤트 발행을 시작하는 Kafka 수신 컴포넌트입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BookEventConsumer {
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final HandleBookRentalEventUseCase handleBookRentalEventUseCase;

    /**
     * 대여 이벤트 JSON을 ItemRented로 읽고, Redis에 eventId를 기록한 뒤 도서를 UNAVAILABLE로 바꾸는 처리를 실행합니다.
     *
     * @param record rental-rent 토픽에서 수신한 대여 이벤트 원본 메시지입니다.
     * @throws Exception JSON 역직렬화, Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book rent eventId={}", event.getEventId());
            return;
        }
        handleBookRentalEventUseCase.handleRent(event);
    }

    /**
     * 반납 이벤트 JSON을 ItemReturned로 읽고, Redis에 eventId를 기록한 뒤 도서를 AVAILABLE로 되돌리는 처리를 실행합니다.
     *
     * @param record rental-return 토픽에서 수신한 반납 이벤트 원본 메시지입니다.
     * @throws Exception JSON 역직렬화, Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ConsumerRecord<String, String> record) throws Exception {
        ItemReturned event = objectMapper.readValue(record.value(), ItemReturned.class);
        if (!markProcessed(event.getEventId())) {
            log.info("skip already processed book return eventId={}", event.getEventId());
            return;
        }
        handleBookRentalEventUseCase.handleReturn(event);
    }

    /**
     * Redis에 처리 이력을 기록해 동일 이벤트가 다시 처리되지 않도록 합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @return 새로 처리할 수 있는 이벤트이면 true, 이미 처리된 이벤트이면 false를 반환합니다.
     */
    private boolean markProcessed(String eventId) {
        String key = "processed:book:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
