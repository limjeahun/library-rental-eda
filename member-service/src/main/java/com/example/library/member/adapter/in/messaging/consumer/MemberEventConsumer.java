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
     * 대여 이벤트를 받아 회원에게 대여 포인트를 적립합니다.
     *
     * <p>수신 토픽: {@code rental_rent} ({@code app.kafka.topics.rental-rent})
     * <p>메시지 타입: {@link ItemRented}
     * <p>발신: rental-service ({@code RentalKafkaEventProducer#publishRentalEvent})
     *
     * @param message rental_rent 토픽에서 수신한 대여 이벤트 메시지.
     * @throws Exception Redis 중복 기록, 회원 포인트 적립 중 오류가 발생할 때 전달.
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
     * 반납 이벤트를 받아 회원에게 반납 포인트를 적립합니다.
     *
     * <p>수신 토픽: {@code rental_return} ({@code app.kafka.topics.rental-return})
     * <p>메시지 타입: {@link ItemReturned}
     * <p>발신: rental-service ({@code RentalKafkaEventProducer#publishReturnEvent})
     *
     * @param message rental_return 토픽에서 수신한 반납 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 적립 중 오류가 발생할 때 전달됩니다.
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
     * 연체 해제 이벤트를 받아 회원 포인트를 연체료만큼 차감합니다.
     *
     * <p>수신 토픽: {@code overdue_clear} ({@code app.kafka.topics.overdue-clear})
     * <p>메시지 타입: {@link OverdueCleared}
     * <p>발신: rental-service ({@code RentalKafkaEventProducer#publishOverdueClearEvent})
     *
     * @param message overdue_clear 토픽에서 수신한 연체 해제 이벤트 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 차감 중 오류가 발생할 때 전달됩니다.
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
     * 포인트 차감 command 를 받아 보상 대상 포인트를 차감합니다.
     *
     * <p>수신 토픽: {@code point_use} ({@code app.kafka.topics.point-use})
     * <p>메시지 타입: {@link PointUseCommand}
     * <p>발신: rental-service ({@code RentalKafkaEventProducer#publishPointUseCommand})
     *
     * @param message point_use 토픽에서 수신한 포인트 차감 command 메시지입니다.
     * @throws Exception Redis 중복 기록, 회원 포인트 차감 중 오류가 발생할 때 전달됩니다.
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
     * 대여 이벤트 메시지의 회원 snapshot 과 포인트 값을 회원 포인트 적립 command 로 변환합니다.
     *
     * @param event common-events 대여 이벤트 record 입니다.
     * @return 회원에게 대여 포인트를 적립하기 위한 application command 를 반환합니다.
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
     * 반납 이벤트 메시지의 회원 snapshot 과 포인트 값을 회원 포인트 적립 command 로 변환합니다.
     *
     * @param event common-events 반납 이벤트 record 입니다.
     * @return 회원에게 반납 포인트를 적립하기 위한 application command 를 반환합니다.
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
     * 연체 해제 이벤트 메시지의 회원 snapshot 과 정산 포인트 값을 회원 포인트 차감 command 로 변환합니다.
     *
     * @param event common-events 연체 해제 이벤트 record 입니다.
     * @return 회원 포인트에서 연체료를 차감하기 위한 application command 를 반환합니다.
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
     * 포인트 사용 command 메시지의 회원 snapshot, 포인트, 사유를 회원 포인트 차감 command 로 변환합니다.
     *
     * @param command common-events 포인트 사용 command record 입니다.
     * @return 보상 흐름에서 회원 포인트를 차감하기 위한 application command 를 반환합니다.
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
     * Redis processing lock handle
     * @param eventId 이벤트 식별자
     * @param message already processing message
     * @param handler 이벤트 handler
     * @throws Exception 예외 전파
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
