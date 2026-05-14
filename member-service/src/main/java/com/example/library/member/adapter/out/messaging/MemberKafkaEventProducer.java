package com.example.library.member.adapter.out.messaging;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.member.application.dto.MemberEventResult;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * member-service 의 포인트 적립/차감 처리 결과를 {@link EventResult} 메시지로 변환해 발행하는 outbound adapter 입니다.
 */
@Component
public class MemberKafkaEventProducer implements PublishMemberEventResultPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalResultTopic;

    public MemberKafkaEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${app.kafka.topics.rental-result}") String rentalResultTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rentalResultTopic = rentalResultTopic;
    }

    /**
     * 회원 포인트 처리 결과를 {@link EventResult} 이벤트로 발행합니다.
     *
     * <p>발행 토픽: {@code rental_result} ({@code app.kafka.topics.rental-result})
     * <p>메시지 타입: {@link EventResult}
     * <p>수신: rental-service ({@code RentalEventConsumer#consumeRentalResult})
     * <p>의미: 회원 서비스가 대여/반납 포인트 적립, 연체료 차감, 보상 포인트 차감을 성공 또는 실패로 처리했음을
     * 대여 흐름에 알리는 결과 이벤트입니다.
     *
     * @param result 회원 서비스의 이벤트 처리 application result 입니다.
     */
    @Override
    public void publish(MemberEventResult result) {
        EventResult eventResult = toEventResult(result);
        kafkaTemplate.send(
            rentalResultTopic,
            eventResult.correlationId(),
            AvroMessageMapper.toEventResultMessage(eventResult)
        );
    }

    private EventResult toEventResult(MemberEventResult result) {
        if (result.successed()) {
            return EventResult.success(
                result.sourceEventId(),
                result.correlationId(),
                result.eventType(),
                result.participant(),
                result.step(),
                result.memberId(),
                result.memberName(),
                result.itemNo(),
                result.itemTitle(),
                result.point()
            );
        }
        return EventResult.failure(
            result.sourceEventId(),
            result.correlationId(),
            result.eventType(),
            result.participant(),
            result.step(),
            result.memberId(),
            result.memberName(),
            result.itemNo(),
            result.itemTitle(),
            result.point(),
            result.reason()
        );
    }
}
