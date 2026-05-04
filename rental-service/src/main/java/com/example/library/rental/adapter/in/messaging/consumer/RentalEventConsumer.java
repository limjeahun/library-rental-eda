package com.example.library.rental.adapter.in.messaging.consumer;

import com.example.library.common.event.EventResult;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 도서 서비스와 회원 서비스가 보낸 처리 결과 이벤트를 받아 실패한 대여 흐름의 보상 처리를 실행하는 Kafka 수신 컴포넌트입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RentalEventConsumer {
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final HandleRentalResultUseCase handleRentalResultUseCase;

    /**
     * 도서/회원 서비스 결과 이벤트 JSON을 EventResult로 읽고, 실패 결과이면 RENT/RETURN/OVERDUE 유형에 맞는 보상 처리를 실행합니다.
     *
     * @param record Kafka에서 수신한 원본 ConsumerRecord 메시지입니다.
     * @throws Exception Kafka 메시지 JSON 역직렬화 또는 실제 업무 처리 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentalResult(ConsumerRecord<String, String> record) throws Exception {
        EventResult result = objectMapper.readValue(record.value(), EventResult.class);
        if (!markProcessed(result.eventId())) {
            log.info("skip already processed rental_result eventId={}", result.eventId());
            return;
        }
        handleRentalResultUseCase.handle(result);
    }

    /**
     * Redis에 처리 이력을 기록해 동일 결과 이벤트가 다시 처리되지 않도록 합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @return 새로 처리할 수 있는 이벤트이면 true, 이미 처리된 이벤트이면 false를 반환합니다.
     */
    private boolean markProcessed(String eventId) {
        String key = "processed:rental:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL));
    }
}
