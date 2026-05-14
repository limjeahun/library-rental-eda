package com.example.library.book.adapter.out.messaging;

import com.example.library.book.application.dto.BookRentalEventResult;
import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.config.BookKafkaTopicProperties;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
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
     * @param result 도서 서비스의 이벤트 처리 application result 입니다.
     */
    @Override
    public void publish(BookRentalEventResult result) {
        EventResult eventResult = toEventResult(result);
        kafkaTemplate.send(
            topicProperties.rentalResult(),
            eventResult.correlationId(),
            AvroMessageMapper.toEventResultMessage(eventResult)
        );
    }

    private EventResult toEventResult(BookRentalEventResult result) {
        if (result.successed()) {
            return EventResult.success(
                result.sourceEventId(),
                result.correlationId(),
                result.eventType(),
                result.participant(),
                result.step(),
                result.memberId(),
                result.memberName(),
                result.itemNo(),
                result.itemTitle(),
                result.point()
            );
        }
        return EventResult.failure(
            result.sourceEventId(),
            result.correlationId(),
            result.eventType(),
            result.participant(),
            result.step(),
            result.memberId(),
            result.memberName(),
            result.itemNo(),
            result.itemTitle(),
            result.point(),
            result.reason()
        );
    }
}
