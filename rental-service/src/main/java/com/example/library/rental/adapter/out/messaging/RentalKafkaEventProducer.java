package com.example.library.rental.adapter.out.messaging;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturnCanceled;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.OverdueClearCanceled;
import com.example.library.common.event.PointUseCommand;
import com.example.library.common.event.PointUseReason;
import com.example.library.rental.application.port.out.PublishItemRentCanceledPort;
import com.example.library.rental.application.port.out.PublishItemRentedPort;
import com.example.library.rental.application.port.out.PublishItemReturnCanceledPort;
import com.example.library.rental.application.port.out.PublishItemReturnedPort;
import com.example.library.rental.application.port.out.PublishOverdueClearCanceledPort;
import com.example.library.rental.application.port.out.PublishOverdueClearedPort;
import com.example.library.rental.application.port.out.PublishPointUseCommandPort;
import com.example.library.rental.config.RentalKafkaTopicProperties;
import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemRentedDomainEvent;
import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemReturnedDomainEvent;
import com.example.library.rental.domain.event.OverdueClearCanceledDomainEvent;
import com.example.library.rental.domain.event.OverdueClearedDomainEvent;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * rental-service 의 도메인 이벤트와 보상 command 를 공통 Kafka 메시지 계약으로 변환해 발행하는 outbound adapter.
 *
 * <p>Kafka eventId는 adapter에서 만들고, 발행 key는 correlationId로 맞춰 같은 흐름을 한 partition에 모읍니다.
 */
@Component
@RequiredArgsConstructor
public class RentalKafkaEventProducer implements PublishItemRentedPort, PublishItemReturnedPort,
    PublishOverdueClearedPort, PublishPointUseCommandPort, PublishItemRentCanceledPort,
    PublishItemReturnCanceledPort, PublishOverdueClearCanceledPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RentalKafkaTopicProperties topicProperties;

    /**
     * 도서 대여가 확정된 사실을 {@link ItemRented} 이벤트로 발행합니다.
     *
     * <p>book/member/bestbook 참여 흐름을 시작합니다.
     *
     * @param event 대여 완료 도메인 이벤트.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishRentalEvent(ItemRentedDomainEvent event, String correlationId) {
        ItemRented message = new ItemRented(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.item().no(),
            event.item().title(),
            event.point()
        );
        kafkaTemplate.send(
                topicProperties.rentalRent(),
                message.correlationId(),
                AvroMessageMapper.toItemRentedMessage(message)
        );
    }

    /**
     * 도서 반납이 확정된 사실을 {@link ItemReturned} 이벤트로 발행합니다.
     *
     * <p>book/member 참여 흐름을 시작합니다.
     *
     * @param event 반납 완료 도메인 이벤트.
     * @param correlationId 반납 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishReturnEvent(ItemReturnedDomainEvent event, String correlationId) {
        ItemReturned message = new ItemReturned(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.item().no(),
            event.item().title(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.rentalReturn(),
            message.correlationId(),
            AvroMessageMapper.toItemReturnedMessage(message)
        );
    }

    /**
     * 연체 해제가 확정된 사실을 {@link OverdueCleared} 이벤트로 발행합니다.
     *
     * <p>member-service의 연체료 포인트 차감을 시작합니다.
     *
     * @param event 연체 해제 완료 도메인 이벤트.
     * @param correlationId 연체 해제 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishOverdueClearEvent(OverdueClearedDomainEvent event, String correlationId) {
        OverdueCleared message = new OverdueCleared(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.overdueClear(),
            message.correlationId(),
            AvroMessageMapper.toOverdueClearedMessage(message)
        );
    }

    /**
     * 대여 실패 보상으로 회원에게 적립된 대여 포인트 차감 command 를 발행합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot.
     * @param point 차감할 대여 포인트.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishRentPointUseCommand(RentalMember member, long point, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        PointUseCommand message = new PointUseCommand(
            eventId,
            normalizeCorrelationId(correlationId, eventId),
            Instant.now(),
            member.id(),
            member.name(),
            point,
            PointUseReason.RENT_COMPENSATION
        );
        kafkaTemplate.send(
            topicProperties.pointUse(),
            message.correlationId(),
            AvroMessageMapper.toPointUseCommandMessage(message)
        );
    }

    /**
     * 반납 실패 보상으로 회원에게 적립된 반납 포인트 차감 command 를 발행합니다.
     *
     * @param member 포인트를 차감할 회원 snapshot.
     * @param point 차감할 반납 포인트.
     * @param correlationId 반납 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishReturnPointUseCommand(RentalMember member, long point, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        PointUseCommand message = new PointUseCommand(
            eventId,
            normalizeCorrelationId(correlationId, eventId),
            Instant.now(),
            member.id(),
            member.name(),
            point,
            PointUseReason.RETURN_COMPENSATION
        );
        kafkaTemplate.send(
            topicProperties.pointUse(),
            message.correlationId(),
            AvroMessageMapper.toPointUseCommandMessage(message)
        );
    }

    /**
     * 대여 실패 보상으로 도서 대여를 취소해야 하는 사실을 {@link ItemRentCanceled} 이벤트로 발행합니다.
     *
     * <p>book/bestbook에 이미 반영된 대여 결과를 되돌리게 합니다.
     *
     * @param event 대여 취소 보상 도메인 이벤트.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishRentCanceledEvent(ItemRentCanceledDomainEvent event, String correlationId) {
        ItemRentCanceled message = new ItemRentCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.item().no(),
            event.item().title(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.rentCancel(),
            message.correlationId(),
            AvroMessageMapper.toItemRentCanceledMessage(message)
        );
    }

    /**
     * 반납 실패 보상으로 도서 반납을 취소해야 하는 사실을 {@link ItemReturnCanceled} 이벤트로 발행합니다.
     *
     * <p>book-service에 이미 반영된 반납 결과를 되돌리게 합니다.
     *
     * @param event 반납 취소 보상 도메인 이벤트.
     * @param correlationId 반납 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishReturnCanceledEvent(ItemReturnCanceledDomainEvent event, String correlationId) {
        ItemReturnCanceled message = new ItemReturnCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.item().no(),
            event.item().title(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.returnCancel(),
            message.correlationId(),
            AvroMessageMapper.toItemReturnCanceledMessage(message)
        );
    }

    /**
     * 연체 해제 실패 보상으로 연체 해제를 취소해야 하는 사실을 {@link OverdueClearCanceled} 이벤트로 발행합니다.
     *
     * @param event 연체 해제 취소 보상 도메인 이벤트.
     * @param correlationId 연체 해제 흐름 전체를 추적하는 상관관계 ID.
     */
    @Override
    public void publishOverdueClearCanceledEvent(
        OverdueClearCanceledDomainEvent event,
        String correlationId
    ) {
        OverdueClearCanceled message = new OverdueClearCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            event.occurredAt(),
            event.member().id(),
            event.member().name(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.overdueClearCancel(),
            message.correlationId(),
            AvroMessageMapper.toOverdueClearCanceledMessage(message)
        );
    }

    /**
     * correlationId가 비어 있으면 eventId로라도 추적 가능하게 만듭니다.
     *
     * @param correlationId 외부 흐름에서 전달된 상관관계 ID.
     * @param eventId 현재 발행할 메시지의 이벤트 ID.
     * @return Kafka 메시지에 사용할 correlationId.
     */
    private String normalizeCorrelationId(String correlationId, String eventId) {
        if (correlationId == null || correlationId.isBlank()) {
            return eventId;
        }
        return correlationId;
    }
}
