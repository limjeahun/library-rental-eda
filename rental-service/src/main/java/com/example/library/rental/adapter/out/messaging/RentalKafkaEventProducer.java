package com.example.library.rental.adapter.out.messaging;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.rental.application.port.out.RentalEventOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 대여/반납/연체해제 이벤트와 포인트 차감 command를 각 Kafka 토픽으로 발행하는 Kafka 발행 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class RentalKafkaEventProducer implements RentalEventOutputPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${app.kafka.topics.rental-rent}")
    private final String rentalRentTopic;
    @Value("${app.kafka.topics.rental-return}")
    private final String rentalReturnTopic;
    @Value("${app.kafka.topics.overdue-clear}")
    private final String overdueClearTopic;
    @Value("${app.kafka.topics.point-use}")
    private final String pointUseTopic;

    /**
     * 도서 대여 완료 이벤트를 대여 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishRentalEvent(ItemRented event) {
        kafkaTemplate.send(rentalRentTopic, event.getCorrelationId(), event);
    }

    /**
     * 도서 반납 완료 이벤트를 반납 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishReturnEvent(ItemReturned event) {
        kafkaTemplate.send(rentalReturnTopic, event.getCorrelationId(), event);
    }

    /**
     * 연체 해제 완료 이벤트를 연체 해제 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishOverdueClearEvent(OverdueCleared event) {
        kafkaTemplate.send(overdueClearTopic, event.getCorrelationId(), event);
    }

    /**
     * 보상 흐름의 포인트 사용 command를 회원 서비스용 토픽으로 발행합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    @Override
    public void publishPointUseCommand(PointUseCommand command) {
        kafkaTemplate.send(pointUseTopic, command.getCorrelationId(), command);
    }
}
