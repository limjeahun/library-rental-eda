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
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointUseCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.config.KafkaConsumerProcessingProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 대여/반납 이벤트와 연체 해제/포인트 차감 메시지를 받아 회원 포인트를 적립하거나 차감하는 Kafka 수신 컴포넌트.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MemberEventConsumer {
    private final StringRedisTemplate               redisTemplate;
    private final HandleMemberEventUseCase          handleMemberEventUseCase;
    private final KafkaConsumerProcessingProperties processingProperties;

    /**
     * 대여 이벤트를 회원 대여 포인트 적립 use case 로 전달합니다.
     *
     * @param message rental_rent 토픽에서 수신한 대여 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-rent}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRent(ItemRentedMessage message) throws Exception {
        ItemRented event = AvroMessageMapper.toItemRented(message);
        handleWithProcessingLock(
                event.eventId(),
                "member rent",
                () -> handleMemberEventUseCase.handleRent(toCommand(event))
        );
    }

    /**
     * 반납 이벤트를 회원 반납 포인트 적립 use case로 전달합니다.
     *
     * @param message rental_return 토픽에서 수신한 반납 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.rental-return}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReturn(ItemReturnedMessage message) throws Exception {
        ItemReturned event = AvroMessageMapper.toItemReturned(message);
        handleWithProcessingLock(
                event.eventId(),
                "member return",
                () -> handleMemberEventUseCase.handleReturn(toCommand(event))
        );
    }

    /**
     * 연체 해제 이벤트를 회원 포인트 차감 use case로 전달합니다.
     *
     * @param message overdue_clear 토픽에서 수신한 연체 해제 이벤트 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.overdue-clear}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeClear(OverdueClearedMessage message) throws Exception {
        OverdueCleared event = AvroMessageMapper.toOverdueCleared(message);
        handleWithProcessingLock(
                event.eventId(),
                "overdue clear",
                () -> handleMemberEventUseCase.handleOverdueClear(toCommand(event))
        );
    }

    /**
     * 포인트 차감 command를 보상 포인트 차감 use case로 전달합니다.
     *
     * @param message point_use 토픽에서 수신한 포인트 차감 command 메시지.
     */
    @KafkaListener(topics = "${app.kafka.topics.point-use}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUsePoint(PointUseCommandMessage message) throws Exception {
        PointUseCommand command = AvroMessageMapper.toPointUseCommand(message);
        handleWithProcessingLock(
                command.eventId(),
                "point use",
                () -> handleMemberEventUseCase.handlePointUse(toCommand(command))
        );
    }

    /**
     * 대여 이벤트 snapshot을 회원 포인트 적립 command로 옮깁니다.
     *
     * @param event common-events 대여 이벤트 record.
     * @return 회원에게 대여 포인트를 적립하기 위한 application command.
     */
    private MemberPointSaveCommand toCommand(ItemRented event) {
        return new MemberPointSaveCommand(
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
     * 반납 이벤트 snapshot을 회원 포인트 적립 command로 옮깁니다.
     *
     * @param event common-events 반납 이벤트 record.
     * @return 회원에게 반납 포인트를 적립하기 위한 application command.
     */
    private MemberPointSaveCommand toCommand(ItemReturned event) {
        return new MemberPointSaveCommand(
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
     * 연체 해제 이벤트 snapshot을 회원 포인트 차감 command로 옮깁니다.
     *
     * @param event common-events 연체 해제 이벤트 record.
     * @return 회원 포인트에서 연체료를 차감하기 위한 application command.
     */
    private MemberOverdueClearCommand toCommand(OverdueCleared event) {
        return new MemberOverdueClearCommand(
            event.eventId(),
            event.correlationId(),
            event.memberId(),
            event.memberName(),
            event.point()
        );
    }

    /**
     * 포인트 사용 command snapshot을 회원 포인트 차감 command로 옮깁니다.
     *
     * @param command common-events 포인트 사용 command record.
     * @return 보상 흐름에서 회원 포인트를 차감하기 위한 application command.
     */
    private MemberPointUseCommand toCommand(PointUseCommand command) {
        return new MemberPointUseCommand(
            command.eventId(),
            command.correlationId(),
            command.memberId(),
            command.memberName(),
            command.point(),
            command.reason() == null ? null : command.reason().name()
        );
    }

    /**
     * Redis 처리 점유권을 얻은 경우에만 handler를 실행합니다.
     *
     * @param eventId 이벤트 식별자.
     * @param message already processing 로그 메시지.
     * @param handler 이벤트 handler.
     */
    private void handleWithProcessingLock(String eventId, String message, MessageHandler handler) throws Exception {
        switch (tryAcquireProcessingLock(eventId)) {
            case CLAIMED -> {
                try {
                    handler.handle();
                } catch (Exception ex) {
                    releaseProcessing(eventId);
                    throw ex;
                }
            }
            case ALREADY_PROCESSING -> log.info("skip already processing {} eventId={}", message, eventId);
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
        return "processing:member:" + eventId;
    }

}
