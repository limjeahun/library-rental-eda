package com.example.library.member.adapter.in.messaging.consumer;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.common.event.schema.ItemRentedMessage;
import com.example.library.common.event.schema.ItemReturnedMessage;
import com.example.library.common.event.schema.OverdueClearedMessage;
import com.example.library.common.event.schema.PointUseCommandMessage;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.config.KafkaConsumerProcessingProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 대여/반납 이벤트와 연체 해제/포인트 차감 메시지를 받아 회원 포인트를 적립하거나 차감하는 Kafka 수신 컴포넌트입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MemberEventConsumer {
    private final StringRedisTemplate               redisTemplate;
    private final HandleMemberEventUseCase          handleMemberEventUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 대여 이벤트 Avro 메시지를 ItemRented 로 변환한 뒤 회원에게 대여 포인트를 적립합니다.
     *
     * @param message rental-rent 토픽에서 수신한 대여 이벤트 메시지.
     * @throws Exception Redis 중복 기록, 회원 포인트 적립 중 오류가 발생할 때 전달.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ItemRentedMessage message) throws Exception {
        ItemRented event = AvroMessageMapper.toItemRented(message);
        // Redis processing lock
        switch (tryAcquireProcessingLock(event.eventId())) {
            case CLAIMED -> {
                try {
                    handleMemberEventUseCase.handleRent(event);
                }catch (Exception ex) {
                    releaseProcessing(event.eventId());
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> {
                log.info("skip already processing member rent eventId={}", event.eventId());
                return;
            }
        }
    }

    /**
     * 반납 이벤트 Avro 메시지를 ItemReturned로 변환한 뒤 회원에게 반납 포인트를 적립합니다.
     *
     * @param message rental-return 토픽에서 수신한 반납 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 적립 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ItemReturnedMessage message) throws Exception {
        ItemReturned event = AvroMessageMapper.toItemReturned(message);
        // Redis processing lock
        switch (tryAcquireProcessingLock(event.eventId())) {
            case CLAIMED -> {
                try {
                    handleMemberEventUseCase.handleReturn(event);
                }catch (Exception ex) {
                    releaseProcessing(event.eventId());
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> {
                log.info("skip already processing member return eventId={}", event.eventId());
                return;
            }
        }
    }

    /**
     * 연체 해제 이벤트 Avro 메시지를 OverdueCleared로 변환한 뒤 회원 포인트를 연체료만큼 차감합니다.
     *
     * @param message overdue-clear 토픽에서 수신한 연체 해제 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 차감 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.overdue-clear}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeClear(OverdueClearedMessage message) throws Exception {
        OverdueCleared event = AvroMessageMapper.toOverdueCleared(message);
        // Redis processing lock
        switch (tryAcquireProcessingLock(event.eventId())) {
            case CLAIMED -> {
                try {
                    handleMemberEventUseCase.handleOverdueClear(event);
                }catch (Exception ex) {
                    releaseProcessing(event.eventId());
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> {
                log.info("skip already processing overdue clear eventId={}", event.eventId());
                return;
            }
        }
    }

    /**
     * 포인트 차감 command Avro 메시지를 PointUseCommand로 변환한 뒤 보상 대상 포인트를 차감합니다.
     *
     * @param message point-use 토픽에서 수신한 포인트 차감 command 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 차감 중 오류가 발생할 때 전달됩니다.
     */
    @KafkaListener(topics = "${app.kafka.topics.point-use}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUsePoint(PointUseCommandMessage message) throws Exception {
        PointUseCommand command = AvroMessageMapper.toPointUseCommand(message);
        // Redis processing lock
        switch (tryAcquireProcessingLock(command.eventId())) {
            case CLAIMED -> {
                try {
                    handleMemberEventUseCase.handlePointUse(command);
                }catch (Exception ex) {
                    releaseProcessing(command.eventId());
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> {
                log.info("skip already processing point_use eventId={}", command.eventId());
                return;
            }
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
        return "processing:member:" + eventId;
    }
}
