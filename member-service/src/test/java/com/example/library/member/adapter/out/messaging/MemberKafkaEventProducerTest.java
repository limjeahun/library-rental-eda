package com.example.library.member.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.common.event.schema.EventResultMessage;
import com.example.library.member.application.dto.MemberPointSaveResultContext;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.vo.MemberIdentity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class MemberKafkaEventProducerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void rentPointSavedDomainEventIsMappedToEventResult() {
        MemberKafkaEventProducer producer = new MemberKafkaEventProducer(kafkaTemplate, "rental-result");
        Instant occurredAt = Instant.parse("2026-05-19T00:00:00Z");
        MemberPointSavedDomainEvent event =
            new MemberPointSavedDomainEvent(occurredAt, new MemberIdentity("member-1", "회원1"), 10L);
        MemberPointSaveResultContext context =
            new MemberPointSaveResultContext("source-event-1", "correlation-1", 1L, "도서1");

        producer.publishRentPointSaved(event, context);

        EventResult result = sentResult("correlation-1");
        assertThat(result.sourceEventId()).isEqualTo("source-event-1");
        assertThat(result.occurredAt()).isEqualTo(occurredAt);
        assertThat(result.eventType()).isEqualTo(EventType.RENT);
        assertThat(result.participant()).isEqualTo(Participant.MEMBER);
        assertThat(result.step()).isEqualTo(SagaStep.MEMBER_SAVE_POINT);
        assertThat(result.successed()).isTrue();
        assertThat(result.memberId()).isEqualTo("member-1");
        assertThat(result.memberName()).isEqualTo("회원1");
        assertThat(result.itemNo()).isEqualTo(1L);
        assertThat(result.itemTitle()).isEqualTo("도서1");
        assertThat(result.point()).isEqualTo(10L);
    }

    private EventResult sentResult(String key) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("rental-result"), eq(key), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(EventResultMessage.class);
        return AvroMessageMapper.toEventResult((EventResultMessage) payloadCaptor.getValue());
    }
}
