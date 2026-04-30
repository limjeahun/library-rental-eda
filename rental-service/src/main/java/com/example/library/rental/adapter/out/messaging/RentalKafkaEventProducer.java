package com.example.library.rental.adapter.out.messaging;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.rental.application.port.out.RentalEventOutputPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RentalKafkaEventProducer implements RentalEventOutputPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalRentTopic;
    private final String rentalReturnTopic;
    private final String overdueClearTopic;
    private final String pointUseTopic;

    public RentalKafkaEventProducer(
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
    public void publishRentalEvent(ItemRented event) {
        kafkaTemplate.send(rentalRentTopic, event.getCorrelationId(), event);
    }

    @Override
    public void publishReturnEvent(ItemReturned event) {
        kafkaTemplate.send(rentalReturnTopic, event.getCorrelationId(), event);
    }

    @Override
    public void publishOverdueClearEvent(OverdueCleared event) {
        kafkaTemplate.send(overdueClearTopic, event.getCorrelationId(), event);
    }

    @Override
    public void publishPointUseCommand(PointUseCommand command) {
        kafkaTemplate.send(pointUseTopic, command.getCorrelationId(), command);
    }
}
