package com.example.library.rental.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;
import org.junit.jupiter.api.Test;

class RentalSagaStateTest {
    @Test
    void rentFlowKeepsPendingParticipantWhenOtherParticipantFails() {
        RentalSagaState state = RentalSagaState.startRent("correlation-1", member(), item(), 10);

        state.recordResult(EventResult.failure(
            "source-1",
            "correlation-1",
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            member().id(),
            member().name(),
            item().no(),
            item().title(),
            10,
            "book failed"
        ));

        assertThat(state.bookResult()).isEqualTo(SagaParticipantStatus.FAILED);
        assertThat(state.memberResult()).isEqualTo(SagaParticipantStatus.PENDING);
        assertThat(state.sagaStatus()).isEqualTo(RentalSagaStatus.COMPENSATING);
    }

    @Test
    void lateParticipantSuccessAfterFailureIsTrackedForCompensation() {
        RentalSagaState state = RentalSagaState.startRent("correlation-1", member(), item(), 10);
        state.recordResult(EventResult.failure(
            "source-1",
            "correlation-1",
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            member().id(),
            member().name(),
            item().no(),
            item().title(),
            10,
            "book failed"
        ));

        state.recordResult(EventResult.success(
            "source-1",
            "correlation-1",
            EventType.RENT,
            Participant.MEMBER,
            SagaStep.MEMBER_SAVE_POINT,
            member().id(),
            member().name(),
            item().no(),
            item().title(),
            10
        ));

        assertThat(state.hasFailure()).isTrue();
        assertThat(state.isMemberSuccess()).isTrue();
        assertThat(state.sagaStatus()).isEqualTo(RentalSagaStatus.COMPENSATED);
    }

    private RentalMember member() {
        return new RentalMember("member-1", "Member 1");
    }

    private RentalItem item() {
        return new RentalItem(1L, "Book 1");
    }
}
