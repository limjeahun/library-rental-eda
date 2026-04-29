package com.example.library.member.framework.kafkaadapter;

import com.example.library.common.event.EventResult;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class MemberEventProducer {
    private static final Logger log = LoggerFactory.getLogger(MemberEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public MemberEventProducer(KafkaTemplate<String, Object> kafkaTemplate, @Value("${app.kafka.topics.rental-result}") String rentalResultTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalResultTopic = rentalResultTopic;
    }

    public void publish(EventResult eventResult) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(rentalResultTopic, eventResult.getEventId(), eventResult);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Member EventResult publish success eventId={} offset={}", eventResult.getEventId(), result.getRecordMetadata().offset());
            } else {
                log.error("Member EventResult publish failed eventId={}", eventResult.getEventId(), ex);
            }
        });
    }
}
