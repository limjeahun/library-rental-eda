package com.example.library.book.adapter.out.messaging;

import com.example.library.book.application.port.out.BookEventOutputPort;
import com.example.library.common.event.EventResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookKafkaEventProducer implements BookEventOutputPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public BookKafkaEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.rental-result}") String rentalResultTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalResultTopic = rentalResultTopic;
    }

    @Override
    public void publish(EventResult result) {
        kafkaTemplate.send(rentalResultTopic, result.getCorrelationId(), result);
    }
}
