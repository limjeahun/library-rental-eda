package com.example.library.rental.framework.kafkaadapter;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.rental.application.outputport.EventOuputPort;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class RentalKafkaProducer implements EventOuputPort {
    private static final Logger log = LoggerFactory.getLogger(RentalKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalRentTopic;
    private final String rentalReturnTopic;
    private final String overdueClearTopic;
    private final String pointUseTopic;

    public RentalKafkaProducer(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.rental-rent}") String rentalRentTopic,
        @Value("${app.kafka.topics.rental-return}") String rentalReturnTopic,
        @Value("${app.kafka.topics.overdue-clear}") String overdueClearTopic,
        @Value("${app.kafka.topics.point-use}") String pointUseTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalRentTopic = rentalRentTopic;
        this.rentalReturnTopic = rentalReturnTopic;
        this.overdueClearTopic = overdueClearTopic;
        this.pointUseTopic = pointUseTopic;
    }

    @Override
    public void occurRentalEvent(ItemRented itemRented) {
        send(rentalRentTopic, itemRented.getEventId(), itemRented);
    }

    @Override
    public void occurRetunEvent(ItemReturned itemReturned) {
        send(rentalReturnTopic, itemReturned.getEventId(), itemReturned);
    }

    @Override
    public void occurOverdueClearEvent(OverdueCleared overdueCleared) {
        send(overdueClearTopic, overdueCleared.getEventId(), overdueCleared);
    }

    @Override
    public void occurPointUseCommand(PointUseCommand pointUseCommand) {
        send(pointUseTopic, pointUseCommand.getEventId(), pointUseCommand);
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka publish success topic={} key={} offset={}", topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Kafka publish failed topic={} key={}, needed to do compensation transaction", topic, key, ex);
            }
        });
    }
}
