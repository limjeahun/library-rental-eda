package com.example.library.book.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.library.book.config.BookKafkaTopicProperties;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;
import com.example.library.common.event.AvroMessageMapper;
import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.common.event.schema.EventResultMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class BookKafkaEventProducerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void bookMadeUnavailableDomainEventIsMappedToEventResult() {
        BookKafkaEventProducer producer = new BookKafkaEventProducer(
            kafkaTemplate,
            new BookKafkaTopicProperties("rental-result")
        );
        BookMadeUnavailableDomainEvent event = new BookMadeUnavailableDomainEvent(1L, "도서1");

        producer.publishBookMadeUnavailable(
            event,
            "source-event-1",
            "correlation-1",
            "member-1",
            "회원1",
            10L
        );

        EventResult result = sentResult("correlation-1");
        assertThat(result.sourceEventId()).isEqualTo("source-event-1");
        assertThat(result.eventType()).isEqualTo(EventType.RENT);
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_UNAVAILABLE);
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
