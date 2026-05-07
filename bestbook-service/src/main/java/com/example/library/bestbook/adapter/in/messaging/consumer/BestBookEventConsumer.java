package com.example.library.bestbook.adapter.in.messaging.consumer;

import com.example.library.bestbook.application.dto.CancelBestBookRentCommand;
import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.bestbook.application.port.in.CancelBestBookRentUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.bestbook.config.KafkaConsumerProcessingProperties;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 도서 대여 이벤트를 받아 도서별 누적 대여 횟수를 인기 도서 MongoDB read model에 반영하는 Kafka 수신 컴포넌트입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BestBookEventConsumer {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final RecordBestBookRentUseCase recordBestBookRentUseCase;
    private final CancelBestBookRentUseCase cancelBestBookRentUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 대여 이벤트 JSON을 ItemRented로 읽고, Redis에 eventId를 기록한 뒤 도서의 누적 대여 횟수를 1 증가시킵니다.
     *
     * @param record rental-rent 토픽에서 수신한 대여 이벤트 원본 메시지입니다.
     * @throws Exception JSON 역직렬화, Redis 중복 기록, 인기 도서 집계 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ConsumerRecord<String, String> record) throws Exception {
        ItemRented event = objectMapper.readValue(record.value(), ItemRented.class);
        if (!claimProcessing(event.eventId())) {
            log.info("skip already processing bestbook eventId={}", event.eventId());
            return;
        }
        try {
            recordBestBookRentUseCase.recordRent(new RecordBestBookRentCommand(
                event.itemNo(),
                event.itemTitle(),
                event.eventId(),
                event.correlationId(),
                InboundMessageType.ITEM_RENTED
            ));
        } finally {
            releaseProcessing(event.eventId());
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.rent-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentCanceled(ConsumerRecord<String, String> record) throws Exception {
        ItemRentCanceled event = objectMapper.readValue(record.value(), ItemRentCanceled.class);
        if (!claimProcessing(event.eventId())) {
            log.info("skip already processing bestbook cancel eventId={}", event.eventId());
            return;
        }
        try {
            cancelBestBookRentUseCase.cancelRent(new CancelBestBookRentCommand(
                event.itemNo(),
                event.eventId(),
                event.correlationId(),
                InboundMessageType.ITEM_RENT_CANCELED
            ));
        } finally {
            releaseProcessing(event.eventId());
        }
    }

    /**
     * Redis에 처리 이력을 기록해 동일 대여 이벤트가 다시 집계되지 않도록 합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @return 새로 처리할 수 있는 이벤트이면 true, 이미 처리된 이벤트이면 false를 반환합니다.
     */
    private boolean claimProcessing(String eventId) {
        String key = processingKey(eventId);
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), processingProperties.ttl())
        );
    }

    private void releaseProcessing(String eventId) {
        redisTemplate.delete(processingKey(eventId));
    }

    private String processingKey(String eventId) {
        return "processing:bestbook:" + eventId;
    }
}
