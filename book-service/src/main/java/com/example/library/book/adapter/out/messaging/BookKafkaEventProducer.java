package com.example.library.book.adapter.out.messaging;

import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.book.config.BookKafkaTopicProperties;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 도서 상태 변경 성공/실패 EventResult 를 rental-result 토픽으로 발행하는 Kafka 발행 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class BookKafkaEventProducer implements PublishBookRentalResultPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookKafkaTopicProperties topicProperties;

    /**
     * 대여 서비스가 수신할 결과 이벤트를 상관관계 ID 키로 발행합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    @Override
    public void publish(EventResult result) {
        kafkaTemplate.send(
            topicProperties.rentalResult(),
            result.correlationId(),
            AvroMessageMapper.toEventResultMessage(result)
        );
    }
}
