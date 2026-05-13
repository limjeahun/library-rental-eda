package com.example.library.book.adapter.in.messaging.consumer;

import com.example.library.book.application.port.in.HandleBookRentalEventUseCase;
import com.example.library.book.config.KafkaConsumerProcessingProperties;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturnCanceled;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.schema.ItemRentCanceledMessage;
import com.example.library.common.event.schema.ItemRentedMessage;
import com.example.library.common.event.schema.ItemReturnCanceledMessage;
import com.example.library.common.event.schema.ItemReturnedMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 대여 서비스가 발행한 대여/반납 이벤트를 받아 도서의 대여 가능 상태 변경과 결과 이벤트 발행을 시작하는 Kafka 수신 컴포넌트.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BookEventConsumer {
    private final StringRedisTemplate redisTemplate;
    private final HandleBookRentalEventUseCase handleBookRentalEventUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 대여 이벤트, 도서를 UNAVAILABLE 로 바꾸는 처리를 실행.
     *
     * @param message rental-rent 토픽에서 수신한 대여 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ItemRentedMessage message) throws Exception {
        ItemRented event = AvroMessageMapper.toItemRented(message);
        handleWithProcessingLock(
                event.eventId(),
                "book rent",
                () -> handleBookRentalEventUseCase.handleRent(event)
        );
    }

    /**
     * 반납 이벤트, 도서를 AVAILABLE 로 되돌리는 처리를 실행.
     *
     * @param message rental-return 토픽에서 수신한 반납 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ItemReturnedMessage message) throws Exception {
        ItemReturned event = AvroMessageMapper.toItemReturned(message);
        handleWithProcessingLock(
                event.eventId(),
                "book return",
                () -> handleBookRentalEventUseCase.handleReturn(event)
        );
    }

    /**
     * 대여 취소 보상 이벤트, 도서를 AVAILABLE 로 되돌리는 처리를 실행.
     *
     * @param message rent-cancel 토픽에서 수신한 대여 취소 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rent-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentCanceled(ItemRentCanceledMessage message) throws Exception {
        ItemRentCanceled event = AvroMessageMapper.toItemRentCanceled(message);
        handleWithProcessingLock(
                event.eventId(),
                "book rent cancel",
                () -> handleBookRentalEventUseCase.handleRentCanceled(event)
        );
    }

    /**
     * 반납 취소 보상 이벤트, 도서를 UNAVAILABLE 로 되돌리는 처리를 실행.
     *
     * @param message return-cancel 토픽에서 수신한 반납 취소 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 도서 상태 변경 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.return-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturnCanceled(ItemReturnCanceledMessage message) throws Exception {
        ItemReturnCanceled event = AvroMessageMapper.toItemReturnCanceled(message);
        handleWithProcessingLock(
                event.eventId(),
                "book return",
                () -> handleBookRentalEventUseCase.handleReturnCanceled(event)
        );
    }

    /**
     * Redis processing lock handle
     * @param eventId 이벤트 식별자
     * @param logType already processing log type
     * @param handler 이벤트 handler
     * @throws Exception 예외 전파
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
     * Redis 에 처리 이력을 기록해 동일 메시지가 다시 처리되지 않도록 합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자.
     * @return 새로 처리할 수 있는 이벤트이면 CLAIMED, 이미 처리된 이벤트이면 ALREADY_PROCESSING 를 반환.
     */
    private ProcessingClaimResult tryAcquireProcessingLock(String eventId) {
        String key = processingKey(eventId);
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), processingProperties.ttl())
        ) ? ProcessingClaimResult.CLAIMED : ProcessingClaimResult.ALREADY_PROCESSING;
    }

    /**
     * Redis processing lock Key 생성 제거.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자.
     */
    private void releaseProcessing(String eventId) {
        redisTemplate.delete(processingKey(eventId));
    }

    /**
     * Redis processing lock Key 생성.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자.
     * @return  Redis processing lock Key.
     */
    private String processingKey(String eventId) {
        return "processing:book:" + eventId;
    }
}
