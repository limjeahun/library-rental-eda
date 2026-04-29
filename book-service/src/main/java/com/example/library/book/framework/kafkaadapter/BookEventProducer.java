package com.example.library.book.framework.kafkaadapter;

import com.example.library.common.event.EventResult;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class BookEventProducer {
    private static final Logger log = LoggerFactory.getLogger(BookEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public BookEventProducer(KafkaTemplate<String, Object> kafkaTemplate, @Value("${app.kafka.topics.rental-result}") String rentalResultTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalResultTopic = rentalResultTopic;
    }

    public void publish(EventResult eventResult) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(rentalResultTopic, eventResult.getEventId(), eventResult);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Book EventResult publish success eventId={} offset={}", eventResult.getEventId(), result.getRecordMetadata().offset());
            } else {
                log.error("Book EventResult publish failed eventId={}", eventResult.getEventId(), ex);
            }
        });
    }
}
