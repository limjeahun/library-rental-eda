package com.example.library.member.adapter.out.messaging;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.dto.MemberPointSaveResultContext;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * member-service 의 포인트 적립/차감 처리 결과를 {@link EventResult} 메시지로 변환해 발행하는 outbound adapter 입니다.
 *
 * <p>Result eventId는 새로 만들고 sourceEventId에는 처리한 원본 메시지 ID를 보존합니다.
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
     * 대여 포인트 적립 성공 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param event 회원 aggregate 가 발생시킨 포인트 적립 도메인 이벤트입니다.
     * @param context 원본 대여 이벤트 ID, correlation ID, 도서 snapshot 을 담은 결과 발행 컨텍스트입니다.
     */
    @Override
    public void publishRentPointSaved(MemberPointSavedDomainEvent event, MemberPointSaveResultContext context) {
        publish(EventResult.success(
            context.sourceEventId(),
            context.correlationId(),
            EventType.RENT,
            Participant.MEMBER,
            SagaStep.MEMBER_SAVE_POINT,
            event.member().id(),
            event.member().name(),
            context.itemNo(),
            context.itemTitle(),
            event.point(),
            event.occurredAt()
        ));
    }

    /**
     * 대여 포인트 적립 실패 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param command 실패한 대여 포인트 적립 command 입니다.
     * @param reason 실패 사유입니다.
     */
    @Override
    public void publishRentPointSaveFailed(MemberPointSaveCommand command, String reason) {
        publish(EventResult.failure(
            command.eventId(),
            command.correlationId(),
            EventType.RENT,
            Participant.MEMBER,
            SagaStep.MEMBER_SAVE_POINT,
            command.memberId(),
            command.memberName(),
            command.itemNo(),
            command.itemTitle(),
            command.point(),
            reason
        ));
    }

    /**
     * 반납 포인트 적립 성공 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param event 회원 aggregate 가 발생시킨 포인트 적립 도메인 이벤트입니다.
     * @param context 원본 반납 이벤트 ID, correlation ID, 도서 snapshot 을 담은 결과 발행 컨텍스트입니다.
     */
    @Override
    public void publishReturnPointSaved(MemberPointSavedDomainEvent event, MemberPointSaveResultContext context) {
        publish(EventResult.success(
            context.sourceEventId(),
            context.correlationId(),
            EventType.RETURN,
            Participant.MEMBER,
            SagaStep.MEMBER_SAVE_POINT,
            event.member().id(),
            event.member().name(),
            context.itemNo(),
            context.itemTitle(),
            event.point(),
            event.occurredAt()
        ));
    }

    /**
     * 반납 포인트 적립 실패 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param command 실패한 반납 포인트 적립 command 입니다.
     * @param reason 실패 사유입니다.
     */
    @Override
    public void publishReturnPointSaveFailed(MemberPointSaveCommand command, String reason) {
        publish(EventResult.failure(
            command.eventId(),
            command.correlationId(),
            EventType.RETURN,
            Participant.MEMBER,
            SagaStep.MEMBER_SAVE_POINT,
            command.memberId(),
            command.memberName(),
            command.itemNo(),
            command.itemTitle(),
            command.point(),
            reason
        ));
    }

    /**
     * 연체 해제 포인트 차감 성공 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param event 회원 aggregate 가 발생시킨 포인트 차감 도메인 이벤트입니다.
     * @param sourceEventId 처리한 원본 연체 해제 이벤트 ID 입니다.
     * @param correlationId 연체 해제 흐름을 연결하는 correlation ID 입니다.
     */
    @Override
    public void publishOverduePointUsed(
        MemberPointUsedDomainEvent event,
        String sourceEventId,
        String correlationId
    ) {
        publish(EventResult.success(
            sourceEventId,
            correlationId,
            EventType.OVERDUE,
            Participant.MEMBER,
            SagaStep.MEMBER_USE_POINT,
            event.member().id(),
            event.member().name(),
            null,
            null,
            event.point(),
            event.occurredAt()
        ));
    }

    /**
     * 연체 해제 포인트 차감 실패 결과를 {@link EventResult}로 만들어 rental-service 에 회신합니다.
     *
     * @param command 실패한 연체 해제 포인트 차감 command 입니다.
     * @param reason 실패 사유입니다.
     */
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

    /**
     * 생성된 {@link EventResult}를 Avro wire payload 로 변환해 rental-result 토픽에 발행합니다.
     *
     * @param eventResult 발행할 회원 이벤트 처리 결과입니다.
     */
    private void publish(EventResult eventResult) {
        kafkaTemplate.send(
            rentalResultTopic,
            eventResult.correlationId(),
            AvroMessageMapper.toEventResultMessage(eventResult)
        );
    }
}
