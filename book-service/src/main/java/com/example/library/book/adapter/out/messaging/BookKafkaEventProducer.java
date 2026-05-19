package com.example.library.book.adapter.out.messaging;

import com.example.library.book.application.dto.BookRentalEventCommand;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.config.BookKafkaTopicProperties;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * book-service 의 대여/반납 이벤트 처리 결과를 {@link EventResult} 메시지로 변환해 발행하는 outbound adapter 입니다.
 */
@Component
@RequiredArgsConstructor
public class BookKafkaEventProducer implements PublishBookRentalResultPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookKafkaTopicProperties topicProperties;

    /**
     * 도서 상태 변경 처리 결과를 {@link EventResult} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code rental_result} ({@code app.kafka.topics.rental-result})
     * <p>메시지 타입: {@link EventResult}
     * <p>수신: rental-service ({@code RentalEventConsumer#consumeRentalResult})
     * <p>의미: 도서 서비스가 대여/반납/보상 이벤트를 성공 또는 실패로 처리했음을 대여 흐름에 알리는 결과 이벤트입니다.
     *
     * @param event 도서 aggregate 가 발생시킨 상태 변경 도메인 이벤트입니다.
     */
    @Override
    public void publishBookMadeUnavailable(
        BookMadeUnavailableDomainEvent event,
        String sourceEventId,
        String correlationId,
        String memberId,
        String memberName,
        long point
    ) {
        publish(success(
            sourceEventId,
            correlationId,
            EventType.RENT,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            memberId,
            memberName,
            event.bookNo(),
            event.title(),
            point,
            event.occurredAt()
        ));
    }

    @Override
    public void publishBookMakeUnavailableFailed(BookRentalEventCommand command, String reason) {
        publish(failure(command, EventType.RENT, SagaStep.BOOK_MAKE_UNAVAILABLE, reason));
    }

    @Override
    public void publishBookMadeAvailable(
        BookMadeAvailableDomainEvent event,
        String sourceEventId,
        String correlationId,
        String memberId,
        String memberName,
        long point
    ) {
        publish(success(
            sourceEventId,
            correlationId,
            EventType.RETURN,
            SagaStep.BOOK_MAKE_AVAILABLE,
            memberId,
            memberName,
            event.bookNo(),
            event.title(),
            point,
            event.occurredAt()
        ));
    }

    @Override
    public void publishBookMakeAvailableFailed(BookRentalEventCommand command, String reason) {
        publish(failure(command, EventType.RETURN, SagaStep.BOOK_MAKE_AVAILABLE, reason));
    }

    private void publish(EventResult eventResult) {
        kafkaTemplate.send(
            topicProperties.rentalResult(),
            eventResult.correlationId(),
            AvroMessageMapper.toEventResultMessage(eventResult)
        );
    }

    private EventResult success(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        SagaStep step,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point,
        Instant occurredAt
    ) {
        return EventResult.success(
            sourceEventId,
            correlationId,
            eventType,
            Participant.BOOK,
            step,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point,
            occurredAt
        );
    }

    private EventResult failure(BookRentalEventCommand command, EventType eventType, SagaStep step, String reason) {
        return EventResult.failure(
            command.eventId(),
            command.correlationId(),
            eventType,
            Participant.BOOK,
            step,
            command.memberId(),
            command.memberName(),
            command.itemNo(),
            command.itemTitle(),
            command.point(),
            reason
        );
    }
}
