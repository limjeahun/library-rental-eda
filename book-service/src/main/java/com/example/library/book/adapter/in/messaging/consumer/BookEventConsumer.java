package com.example.library.book.adapter.in.messaging.consumer;

import com.example.library.book.application.dto.BookRentalCancelCommand;
import com.example.library.book.application.dto.BookRentalEventCommand;
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
     * 대여 이벤트를 도서 대여 불가 처리 use case로 전달합니다.
     *
     * @param message rental_rent 토픽에서 수신한 대여 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ItemRentedMessage message) throws Exception {
        ItemRented event = AvroMessageMapper.toItemRented(message);
        handleWithProcessingLock(
                event.eventId(),
                "book rent",
                () -> handleBookRentalEventUseCase.handleRent(toCommand(event))
        );
    }

    /**
     * 반납 이벤트를 도서 대여 가능 처리 use case로 전달합니다.
     *
     * @param message rental_return 토픽에서 수신한 반납 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ItemReturnedMessage message) throws Exception {
        ItemReturned event = AvroMessageMapper.toItemReturned(message);
        handleWithProcessingLock(
                event.eventId(),
                "book return",
                () -> handleBookRentalEventUseCase.handleReturn(toCommand(event))
        );
    }

    /**
     * 대여 취소 이벤트를 도서 대여 가능 보상 use case로 전달합니다.
     *
     * @param message rent_cancel 토픽에서 수신한 대여 취소 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rent-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentCanceled(ItemRentCanceledMessage message) throws Exception {
        ItemRentCanceled event = AvroMessageMapper.toItemRentCanceled(message);
        handleWithProcessingLock(
                event.eventId(),
                "book rent cancel",
                () -> handleBookRentalEventUseCase.handleRentCanceled(toCommand(event))
        );
    }

    /**
     * 반납 취소 이벤트를 도서 대여 불가 보상 use case로 전달합니다.
     *
     * @param message return_cancel 토픽에서 수신한 반납 취소 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.return-cancel}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturnCanceled(ItemReturnCanceledMessage message) throws Exception {
        ItemReturnCanceled event = AvroMessageMapper.toItemReturnCanceled(message);
        handleWithProcessingLock(
                event.eventId(),
                "book return",
                () -> handleBookRentalEventUseCase.handleReturnCanceled(toCommand(event))
        );
    }

    /**
     * 대여 이벤트 snapshot을 도서 상태 변경 command로 옮깁니다.
     *
     * @param event common-events 대여 이벤트 record.
     * @return 도서를 대여 불가능 상태로 변경하기 위한 application command.
     */
    private BookRentalEventCommand toCommand(ItemRented event) {
        return new BookRentalEventCommand(
            event.eventId(),
            event.correlationId(),
            event.memberId(),
            event.memberName(),
            event.itemNo(),
            event.itemTitle(),
            event.point()
        );
    }

    /**
     * 반납 이벤트 snapshot을 도서 상태 변경 command로 옮깁니다.
     *
     * @param event common-events 반납 이벤트 record.
     * @return 도서를 대여 가능 상태로 변경하기 위한 application command.
     */
    private BookRentalEventCommand toCommand(ItemReturned event) {
        return new BookRentalEventCommand(
            event.eventId(),
            event.correlationId(),
            event.memberId(),
            event.memberName(),
            event.itemNo(),
            event.itemTitle(),
            event.point()
        );
    }

    /**
     * 대여 취소 이벤트 snapshot을 도서 보상 command로 옮깁니다.
     *
     * @param event common-events 대여 취소 이벤트 record.
     * @return 도서를 대여 가능 상태로 되돌리기 위한 application command.
     */
    private BookRentalCancelCommand toCommand(ItemRentCanceled event) {
        return new BookRentalCancelCommand(event.eventId(), event.correlationId(), event.itemNo());
    }

    /**
     * 반납 취소 이벤트 snapshot을 도서 보상 command로 옮깁니다.
     *
     * @param event common-events 반납 취소 이벤트 record.
     * @return 도서를 대여 불가능 상태로 되돌리기 위한 application command.
     */
    private BookRentalCancelCommand toCommand(ItemReturnCanceled event) {
        return new BookRentalCancelCommand(event.eventId(), event.correlationId(), event.itemNo());
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
        return "processing:book:" + eventId;
    }
}
