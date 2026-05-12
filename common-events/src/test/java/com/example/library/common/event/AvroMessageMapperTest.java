package com.example.library.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AvroMessageMapperTest {
    @Test
    void itemRentedRoundTripKeepsSnapshotFields() {
        ItemRented event = new ItemRented(
            "event-1",
            "correlation-1",
            Instant.parse("2026-05-13T00:00:00Z"),
            "member-1",
            "Member 1",
            1L,
            "Book 1",
            10
        );

        ItemRented mapped = AvroMessageMapper.toItemRented(
            AvroMessageMapper.toItemRentedMessage(event)
        );

        assertThat(mapped).isEqualTo(event);
    }

    @Test
    void eventResultRoundTripKeepsProtocolFieldsAndNullableReason() {
        EventResult result = new EventResult(
            "event-1",
            "correlation-1",
            "source-event-1",
            Instant.parse("2026-05-13T00:00:00.123Z"),
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            false,
            "member-1",
            "Member 1",
            1L,
            "Book 1",
            10,
            "forced failure"
        );

        EventResult mapped = AvroMessageMapper.toEventResult(
            AvroMessageMapper.toEventResultMessage(result)
        );

        assertThat(mapped).isEqualTo(result);
    }

    @Test
    void overdueClearedRoundTripUsesEpochMillisForOccurredAt() {
        OverdueCleared event = new OverdueCleared(
            "event-1",
            "correlation-1",
            Instant.parse("2026-05-13T00:00:00.123Z"),
            "member-1",
            "Member 1",
            30
        );

        OverdueCleared mapped = AvroMessageMapper.toOverdueCleared(
            AvroMessageMapper.toOverdueClearedMessage(event)
        );

        assertThat(mapped).isEqualTo(event);
    }
}
