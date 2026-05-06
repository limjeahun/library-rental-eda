package com.example.library.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EventResultTest {
    @Test
    void successCreatesNewEventIdAndKeepsSourceEventMetadata() {
        EventResult result = EventResult.success(
            "source-event-1",
            "correlation-1",
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            "member-1",
            "Member 1",
            1L,
            "Book 1",
            10
        );

        assertThat(result.eventId()).isNotBlank();
        assertThat(result.eventId()).isNotEqualTo(result.sourceEventId());
        assertThat(result.sourceEventId()).isEqualTo("source-event-1");
        assertThat(result.correlationId()).isEqualTo("correlation-1");
        assertThat(result.participant()).isEqualTo(Participant.BOOK);
        assertThat(result.step()).isEqualTo(SagaStep.BOOK_MAKE_UNAVAILABLE);
        assertThat(result.successed()).isTrue();
        assertThat(result.memberId()).isEqualTo("member-1");
        assertThat(result.itemNo()).isEqualTo(1L);
    }

    @Test
    void rentResultRequiresItemSnapshot() {
        assertThatThrownBy(() -> EventResult.success(
            "source-event-1",
            "correlation-1",
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            "member-1",
            "Member 1",
            null,
            "Book 1",
            10
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overdueResultAllowsEmptyItemSnapshot() {
        EventResult result = EventResult.success(
            "source-event-1",
            "correlation-1",
            EventType.OVERDUE,
            Participant.MEMBER,
            SagaStep.MEMBER_USE_POINT,
            "member-1",
            "Member 1",
            null,
            null,
            10
        );

        assertThat(result.itemNo()).isNull();
        assertThat(result.itemTitle()).isNull();
    }
}
