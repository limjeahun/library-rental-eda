package com.example.library.member.adapter.out.messaging;

import com.example.library.common.event.EventResult;
import com.example.library.member.application.port.out.MemberEventOutputPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MemberKafkaEventProducer implements MemberEventOutputPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public MemberKafkaEventProducer(
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
