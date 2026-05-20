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
     * 참여 서비스 결과 이벤트를 대여 SAGA 결과 처리 use case로 전달합니다.
     *
     * @param message rental_result 토픽에서 수신한 결과 이벤트 메시지.
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
     * 결과 이벤트 snapshot을 보상 판단 command로 옮깁니다.
     *
     * @param result common-events 참여 서비스 결과 이벤트 record.
     * @return 대여 흐름의 성공/실패 기록과 보상 여부를 판단하기 위한 application command.
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
     * Redis 처리 점유권을 얻은 경우에만 handler를 실행합니다.
     *
     * @param eventId 이벤트 식별자.
     * @param logMessage already processing 로그 메시지.
     * @param handler 이벤트 handler.
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
        return "processing:rental:" + eventId;
    }
}
