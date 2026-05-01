package com.example.library.book.adapter.out.messaging;

import com.example.library.book.application.port.out.PublishBookRentalResultPort;
import com.example.library.common.event.EventResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 도서 상태 변경 성공/실패 EventResult를 rental-result 토픽으로 발행하는 Kafka 발행 컴포넌트입니다.
 */
@Component
public class BookKafkaEventProducer implements PublishBookRentalResultPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public BookKafkaEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.rental-result}") String rentalResultTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalResultTopic = rentalResultTopic;
    }

    /**
     * 대여 서비스가 수신할 결과 이벤트를 상관관계 ID 키로 발행합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     */
    @Override
    public void publish(EventResult result) {
        kafkaTemplate.send(rentalResultTopic, result.getCorrelationId(), result);
    }
}
