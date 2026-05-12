package com.example.library.rental.adapter.out.messaging;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.ItemRentCanceled;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturnCanceled;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.OverdueClearCanceled;
import com.example.library.common.event.PointUseCommand;
import com.example.library.rental.application.dto.PointUseCommandPayload;
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
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 대여/반납/연체해제 이벤트와 포인트 차감 command를 각 Kafka 토픽으로 발행하는 Kafka 발행 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class RentalKafkaEventProducer implements PublishItemRentedPort, PublishItemReturnedPort,
    PublishOverdueClearedPort, PublishPointUseCommandPort, PublishItemRentCanceledPort,
    PublishItemReturnCanceledPort, PublishOverdueClearCanceledPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RentalKafkaTopicProperties topicProperties;

    /**
     * 도서 대여 완료 이벤트를 대여 토픽으로 발행.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지.
     */
    @Override
    public void publishRentalEvent(ItemRentedDomainEvent event, String correlationId) {
        ItemRented message = new ItemRented(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
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
     * 도서 반납 완료 이벤트를 반납 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishReturnEvent(ItemReturnedDomainEvent event, String correlationId) {
        ItemReturned message = new ItemReturned(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
            event.idName().id(),
            event.idName().name(),
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
     * 연체 해제 완료 이벤트를 연체 해제 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishOverdueClearEvent(OverdueClearedDomainEvent event, String correlationId) {
        OverdueCleared message = new OverdueCleared(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
            event.idName().id(),
            event.idName().name(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.overdueClear(),
            message.correlationId(),
            AvroMessageMapper.toOverdueClearedMessage(message)
        );
    }

    /**
     * 보상 흐름의 포인트 사용 command를 회원 서비스용 토픽으로 발행합니다.
     *
     * @param command 포인트를 변경할 회원 snapshot과 포인트 금액을 담은 command payload입니다.
     */
    @Override
    public void publishPointUseCommand(PointUseCommandPayload command) {
        String eventId = UUID.randomUUID().toString();
        PointUseCommand message = new PointUseCommand(
            eventId,
            normalizeCorrelationId(command.correlationId(), eventId),
            Instant.now(),
            command.memberId(),
            command.memberName(),
            command.point(),
            command.reason()
        );
        kafkaTemplate.send(
            topicProperties.pointUse(),
            message.correlationId(),
            AvroMessageMapper.toPointUseCommandMessage(message)
        );
    }

    @Override
    public void publishRentCanceledEvent(ItemRentCanceledDomainEvent event, String correlationId) {
        ItemRentCanceled message = new ItemRentCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
            event.idName().id(),
            event.idName().name(),
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

    @Override
    public void publishReturnCanceledEvent(ItemReturnCanceledDomainEvent event, String correlationId) {
        ItemReturnCanceled message = new ItemReturnCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
            event.idName().id(),
            event.idName().name(),
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

    @Override
    public void publishOverdueClearCanceledEvent(
        OverdueClearCanceledDomainEvent event,
        String correlationId
    ) {
        OverdueClearCanceled message = new OverdueClearCanceled(
            UUID.randomUUID().toString(),
            correlationId,
            Instant.now(),
            event.idName().id(),
            event.idName().name(),
            event.point()
        );
        kafkaTemplate.send(
            topicProperties.overdueClearCancel(),
            message.correlationId(),
            AvroMessageMapper.toOverdueClearCanceledMessage(message)
        );
    }

    private String normalizeCorrelationId(String correlationId, String eventId) {
        if (correlationId == null || correlationId.isBlank()) {
            return eventId;
        }
        return correlationId;
    }
}
