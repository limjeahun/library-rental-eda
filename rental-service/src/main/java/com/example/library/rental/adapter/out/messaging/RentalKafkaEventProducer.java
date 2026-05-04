package com.example.library.rental.adapter.out.messaging;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.rental.application.port.out.PublishItemRentedPort;
import com.example.library.rental.application.port.out.PublishItemReturnedPort;
import com.example.library.rental.application.port.out.PublishOverdueClearedPort;
import com.example.library.rental.application.port.out.PublishPointUseCommandPort;
import com.example.library.rental.config.RentalKafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 대여/반납/연체해제 이벤트와 포인트 차감 command를 각 Kafka 토픽으로 발행하는 Kafka 발행 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class RentalKafkaEventProducer implements PublishItemRentedPort, PublishItemReturnedPort,
    PublishOverdueClearedPort, PublishPointUseCommandPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RentalKafkaTopicProperties topicProperties;

    /**
     * 도서 대여 완료 이벤트를 대여 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishRentalEvent(ItemRented event) {
        kafkaTemplate.send(topicProperties.rentalRent(), event.correlationId(), event);
    }

    /**
     * 도서 반납 완료 이벤트를 반납 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishReturnEvent(ItemReturned event) {
        kafkaTemplate.send(topicProperties.rentalReturn(), event.correlationId(), event);
    }

    /**
     * 연체 해제 완료 이벤트를 연체 해제 토픽으로 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    @Override
    public void publishOverdueClearEvent(OverdueCleared event) {
        kafkaTemplate.send(topicProperties.overdueClear(), event.correlationId(), event);
    }

    /**
     * 보상 흐름의 포인트 사용 command를 회원 서비스용 토픽으로 발행합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    @Override
    public void publishPointUseCommand(PointUseCommand command) {
        kafkaTemplate.send(topicProperties.pointUse(), command.correlationId(), command);
    }
}
