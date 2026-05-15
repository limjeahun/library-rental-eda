package com.example.library.member.adapter.out.messaging;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;
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
     * @param event 회원 aggregate 가 발생시킨 포인트 적립 도메인 이벤트입니다.
     */
    @Override
    public void publishRentPointSaved(
        MemberPointSavedDomainEvent event,
        String sourceEventId,
        String correlationId,
        Long itemNo,
        String itemTitle
    ) {
        publish(success(
            sourceEventId,
            correlationId,
            EventType.RENT,
            SagaStep.MEMBER_SAVE_POINT,
            event.member().id(),
            event.member().name(),
            itemNo,
            itemTitle,
            event.point()
        ));
    }

    @Override
    public void publishRentPointSaveFailed(MemberPointSaveCommand command, String reason) {
        publish(failure(command, EventType.RENT, SagaStep.MEMBER_SAVE_POINT, reason));
    }

    @Override
    public void publishReturnPointSaved(
        MemberPointSavedDomainEvent event,
        String sourceEventId,
        String correlationId,
        Long itemNo,
        String itemTitle
    ) {
        publish(success(
            sourceEventId,
            correlationId,
            EventType.RETURN,
            SagaStep.MEMBER_SAVE_POINT,
            event.member().id(),
            event.member().name(),
            itemNo,
            itemTitle,
            event.point()
        ));
    }

    @Override
    public void publishReturnPointSaveFailed(MemberPointSaveCommand command, String reason) {
        publish(failure(command, EventType.RETURN, SagaStep.MEMBER_SAVE_POINT, reason));
    }

    @Override
    public void publishOverduePointUsed(
        MemberPointUsedDomainEvent event,
        String sourceEventId,
        String correlationId
    ) {
        publish(success(
            sourceEventId,
            correlationId,
            EventType.OVERDUE,
            SagaStep.MEMBER_USE_POINT,
            event.member().id(),
            event.member().name(),
            null,
            null,
            event.point()
        ));
    }

    @Override
    public void publishOverduePointUseFailed(MemberOverdueClearCommand command, String reason) {
        publish(EventResult.failure(
            command.eventId(),
            command.correlationId(),
            EventType.OVERDUE,
            Participant.MEMBER,
            SagaStep.MEMBER_USE_POINT,
            command.memberId(),
            command.memberName(),
            null,
            null,
            command.point(),
            reason
        ));
    }

    private void publish(EventResult eventResult) {
        kafkaTemplate.send(
            rentalResultTopic,
            eventResult.correlationId(),
            AvroMessageMapper.toEventResultMessage(eventResult)
        );
    }

    private EventResult success(
        String sourceEventId,
        String correlationId,
        EventType eventType,
        SagaStep step,
        String memberId,
        String memberName,
        Long itemNo,
        String itemTitle,
        long point
    ) {
        return EventResult.success(
            sourceEventId,
            correlationId,
            eventType,
            Participant.MEMBER,
            step,
            memberId,
            memberName,
            itemNo,
            itemTitle,
            point
        );
    }

    private EventResult failure(MemberPointSaveCommand command, EventType eventType, SagaStep step, String reason) {
        return EventResult.failure(
            command.eventId(),
            command.correlationId(),
            eventType,
            Participant.MEMBER,
            step,
            command.memberId(),
            command.memberName(),
            command.itemNo(),
            command.itemTitle(),
            command.point(),
            reason
        );
    }
}
