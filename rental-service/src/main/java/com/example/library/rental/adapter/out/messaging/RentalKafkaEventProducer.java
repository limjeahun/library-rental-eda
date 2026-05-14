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
 * rental-service 의 도메인 이벤트와 보상 command 를 공통 Kafka 메시지 계약으로 변환해 발행하는 outbound adapter.
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
     * <p>발행 토픽: {@code rental_rent} ({@code app.kafka.topics.rental-rent})
     * <p>메시지 타입: {@link ItemRented}
     * <p>수신: book-service ({@code BookEventConsumer#consumeRent}),
     * member-service ({@code MemberEventConsumer#consumeRent}),
     * bestbook-service ({@code BestBookEventConsumer#consumeRent})
     * <p>의미: 도서 상태 변경, 회원 대여 포인트 적립, 인기 도서 집계를 시작하는 대여 완료 이벤트입니다.
     *
     * @param event 대여 완료 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
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
     * 도서 반납이 확정된 사실을 {@link ItemReturned} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code rental_return} ({@code app.kafka.topics.rental-return})
     * <p>메시지 타입: {@link ItemReturned}
     * <p>수신: book-service ({@code BookEventConsumer#consumeReturn}),
     * member-service ({@code MemberEventConsumer#consumeReturn})
     * <p>의미: 도서 상태를 대여 가능으로 되돌리고 회원 반납 포인트를 적립하기 위한 반납 완료 이벤트입니다.
     *
     * @param event 반납 완료 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
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
     * 연체 해제가 확정된 사실을 {@link OverdueCleared} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code overdue_clear} ({@code app.kafka.topics.overdue-clear})
     * <p>메시지 타입: {@link OverdueCleared}
     * <p>수신: member-service ({@code MemberEventConsumer#consumeClear})
     * <p>의미: 연체 해제에 따른 회원 포인트 차감을 요청하는 연체 해제 완료 이벤트입니다.
     *
     * @param event 연체 해제 완료 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
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
     * 보상 흐름에서 회원 포인트를 차감하기 위한 {@link PointUseCommand} command 를 발행합니다.
     *
     * <p>발행 토픽: {@code point_use} ({@code app.kafka.topics.point-use})
     * <p>메시지 타입: {@link PointUseCommand}
     * <p>수신: member-service ({@code MemberEventConsumer#consumeUsePoint})
     * <p>의미: 대여 흐름 보상 과정에서 회원에게 이미 적립된 포인트를 되돌리도록 요청하는 command 입니다.
     *
     * @param command 포인트를 변경할 회원 snapshot 과 포인트 금액을 담은 command payload 입니다.
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

    /**
     * 대여 실패 보상으로 도서 대여를 취소해야 하는 사실을 {@link ItemRentCanceled} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code rent_cancel} ({@code app.kafka.topics.rent-cancel})
     * <p>메시지 타입: {@link ItemRentCanceled}
     * <p>수신: book-service ({@code BookEventConsumer#consumeRentCanceled}),
     * bestbook-service ({@code BestBookEventConsumer#consumeRentCanceled})
     * <p>의미: 도서를 다시 대여 가능 상태로 되돌리고 인기 도서 집계를 차감하기 위한 대여 취소 보상 이벤트입니다.
     *
     * @param event 대여 취소 보상 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
     */
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

    /**
     * 반납 실패 보상으로 도서 반납을 취소해야 하는 사실을 {@link ItemReturnCanceled} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code return_cancel} ({@code app.kafka.topics.return-cancel})
     * <p>메시지 타입: {@link ItemReturnCanceled}
     * <p>수신: book-service ({@code BookEventConsumer#consumeReturnCanceled})
     * <p>의미: 도서를 다시 대여 불가 상태로 되돌리기 위한 반납 취소 보상 이벤트입니다.
     *
     * @param event 반납 취소 보상 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
     */
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

    /**
     * 연체 해제 실패 보상으로 연체 해제를 취소해야 하는 사실을 {@link OverdueClearCanceled} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code overdue_clear_cancel} ({@code app.kafka.topics.overdue-clear-cancel})
     * <p>메시지 타입: {@link OverdueClearCanceled}
     * <p>수신: 현재 {@code *EventConsumer} 수신 없음
     * <p>의미: 연체 해제 보상 흐름을 Kafka 메시지로 표현하기 위한 연체 해제 취소 보상 이벤트입니다.
     *
     * @param event 연체 해제 취소 보상 도메인 이벤트입니다.
     * @param correlationId 대여 흐름 전체를 추적하는 상관관계 ID입니다.
     */
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

    /**
     * 발행할 command 의 correlationId 가 비어 있을 때 현재 메시지의 eventId 를 대체 correlationId 로 사용합니다.
     *
     * <p>기존 correlationId 가 있으면 비동기 대여 흐름 추적을 위해 그대로 유지하고, 없거나 blank 이면
     * 새로 생성한 eventId 를 반환해 메시지가 최소한 자기 eventId 로 추적될 수 있게 합니다.
     *
     * @param correlationId 외부 흐름에서 전달된 상관관계 ID입니다.
     * @param eventId 현재 발행할 메시지의 이벤트 ID입니다.
     * @return Kafka 메시지에 사용할 correlationId 입니다.
     */
    private String normalizeCorrelationId(String correlationId, String eventId) {
        if (correlationId == null || correlationId.isBlank()) {
            return eventId;
        }
        return correlationId;
    }
}
