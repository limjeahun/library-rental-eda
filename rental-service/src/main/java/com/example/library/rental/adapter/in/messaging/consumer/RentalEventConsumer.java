package com.example.library.rental.adapter.in.messaging.consumer;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.schema.EventResultMessage;
import com.example.library.rental.application.dto.RentalResultCommand;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import com.example.library.rental.config.KafkaConsumerProcessingProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 도서 서비스와 회원 서비스가 보낸 처리 결과 이벤트를 받아 실패한 대여 흐름의 보상 처리를 실행하는 Kafka 수신 컴포넌트.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RentalEventConsumer {
    private final StringRedisTemplate redisTemplate;
    private final HandleRentalResultUseCase handleRentalResultUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 도서/회원 서비스 결과 이벤트를 받아 대여 흐름의 보상 처리를 실행합니다.
     *
     * <p>수신 토픽: {@code rental_result} ({@code app.kafka.topics.rental-result})
     * <p>메시지 타입: {@link EventResult}
     * <p>발신: book-service ({@code BookKafkaEventProducer#publish}),
     * member-service ({@code MemberKafkaEventProducer#publish})
     *
     * @param message rental_result 토픽에서 수신한 결과 이벤트 메시지입니다.
     * @throws Exception 실제 업무 처리 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRentalResult(EventResultMessage message) throws Exception {
        EventResult result = AvroMessageMapper.toEventResult(message);
        handleWithProcessingLock(
                result.eventId(),
                "rental result",
                () -> handleRentalResultUseCase.handle(toCommand(result))
        );
    }

    /**
     * 참여 서비스 결과 이벤트의 workflow metadata 와 업무 snapshot 을 보상 판단 command 로 변환합니다.
     *
     * @param result common-events 참여 서비스 결과 이벤트 record 입니다.
     * @return 대여 흐름의 성공/실패 기록과 보상 여부를 판단하기 위한 application command 를 반환합니다.
     */
    private RentalResultCommand toCommand(EventResult result) {
        return new RentalResultCommand(
            result.eventId(),
            result.correlationId(),
            result.sourceEventId(),
            result.eventType(),
            result.participant(),
            result.step(),
            result.successed(),
            result.memberId(),
            result.memberName(),
            result.itemNo(),
            result.itemTitle(),
            result.point(),
            result.reason()
        );
    }

    /**
     * Redis processing lock handle
     * @param eventId 이벤트 식별자
     * @param logMessage already processing log message
     * @param handler 이벤트 handler
     * @throws Exception 예외 전파
     */
    private void handleWithProcessingLock(String eventId, String logMessage, MessageHandler handler) throws Exception {
        switch (tryAcquireProcessingLock(eventId)) {
            case CLAIMED -> {
                try {
                    handler.handle();
                } catch (Exception ex) {
                    releaseProcessing(eventId);
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> log.info("skip already processing {} eventId={}", logMessage, eventId);
        }
    }

    /**
     * Redis 에 처리 이력을 기록해 동일 결과 이벤트가 다시 처리되지 않도록 합니다.
     *
     * @param eventId 이벤트 식별자.
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
     * @param eventId 이벤트 식별자.
     */
    private void releaseProcessing(String eventId) {
        redisTemplate.delete(processingKey(eventId));
    }

    /**
     * Redis processing lock Key 생성.
     *
     * @param eventId 이벤트 식별자.
     * @return  Redis processing lock Key.
     */
    private String processingKey(String eventId) {
        return "processing:rental:" + eventId;
    }
}
