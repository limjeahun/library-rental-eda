package com.example.library.bestbook.adapter.in.messaging.consumer;

import com.example.library.bestbook.application.dto.CancelBestBookRentCommand;
import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.bestbook.application.port.in.CancelBestBookRentUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.bestbook.config.KafkaConsumerProcessingProperties;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.schema.ItemRentCanceledMessage;
import com.example.library.common.event.schema.ItemRentedMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 도서 대여 이벤트를 받아 도서별 누적 대여 횟수를 인기 도서 MongoDB read model 에 반영하는 Kafka 수신 컴포넌트.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BestBookEventConsumer {
    private final StringRedisTemplate redisTemplate;
    private final RecordBestBookRentUseCase recordBestBookRentUseCase;
    private final CancelBestBookRentUseCase cancelBestBookRentUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 대여 이벤트를 인기 도서 대여 횟수 증가 use case로 전달합니다.
     *
     * @param message rental_rent 토픽에서 수신한 대여 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ItemRentedMessage message) throws Exception {
        ItemRented event = AvroMessageMapper.toItemRented(message);
        handleWithProcessingLock(
                event.eventId(),
                "best-book",
                () -> recordBestBookRentUseCase.recordRent(toCommand(event))
        );
    }

    /**
     * 대여 취소 이벤트를 인기 도서 대여 횟수 감소 use case로 전달합니다.
     *
     * @param message rent_cancel 토픽에서 수신한 대여 취소 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rent-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentCanceled(ItemRentCanceledMessage message) throws Exception {
        ItemRentCanceled event = AvroMessageMapper.toItemRentCanceled(message);
        handleWithProcessingLock(
                event.eventId(),
                "best-book cancel",
                () -> cancelBestBookRentUseCase.cancelRent(toCommand(event))
        );
    }

    /**
     * 대여 이벤트 snapshot을 인기 도서 집계 command로 옮깁니다.
     *
     * @param event common-events 대여 이벤트 record.
     * @return 인기 도서 누적 대여 횟수를 증가시키기 위한 application command.
     */
    private RecordBestBookRentCommand toCommand(ItemRented event) {
        return new RecordBestBookRentCommand(
            event.itemNo(),
            event.itemTitle(),
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RENTED
        );
    }

    /**
     * 대여 취소 이벤트 snapshot을 인기 도서 집계 취소 command로 옮깁니다.
     *
     * @param event common-events 대여 취소 이벤트 record.
     * @return 인기 도서 누적 대여 횟수를 감소시키기 위한 application command.
     */
    private CancelBestBookRentCommand toCommand(ItemRentCanceled event) {
        return new CancelBestBookRentCommand(
            event.itemNo(),
            event.eventId(),
            event.correlationId(),
            InboundMessageType.ITEM_RENT_CANCELED
        );
    }

    /**
     * Redis 처리 점유권을 얻은 경우에만 handler를 실행합니다.
     *
     * @param eventId 이벤트 식별자.
     * @param logType already processing 로그 타입.
     * @param handler 이벤트 handler.
     */
    private void handleWithProcessingLock(String eventId, String logType, MessageHandler handler) throws Exception {
        switch (tryAcquireProcessingLock(eventId)) {
            case CLAIMED -> {
                try {
                    handler.handle();
                } catch (Exception ex) {
                    releaseProcessing(eventId);
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> log.info("skip already processing {} eventId={}", logType, eventId);
        }
    }

    /**
     * Redis lock은 동시 처리만 막고, 최종 중복 방지는 processed message 저장소가 맡습니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자.
     * @return 새로 처리할 수 있으면 CLAIMED, 이미 처리 중이면 ALREADY_PROCESSING.
     */
    private ProcessingClaimResult tryAcquireProcessingLock(String eventId) {
        String key = processingKey(eventId);
        // 동시에 처리 중인 consumer만 막고, 실패하면 releaseProcessing으로 다시 열어둡니다.
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), processingProperties.ttl())
        ) ? ProcessingClaimResult.CLAIMED : ProcessingClaimResult.ALREADY_PROCESSING;
    }

    /**
     * 처리 실패 시 재전달 메시지가 다시 점유할 수 있도록 lock을 해제합니다.
     *
     * @param eventId 이벤트 식별자.
     */
    private void releaseProcessing(String eventId) {
        redisTemplate.delete(processingKey(eventId));
    }

    /**
     * 서비스별 처리 lock key를 만듭니다.
     *
     * @param eventId 이벤트 식별자.
     * @return Redis processing lock key.
     */
    private String processingKey(String eventId) {
        return "processing:bestbook:" + eventId;
    }
}
