package com.example.library.rental.domain.model.saga;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import org.junit.jupiter.api.Test;

class RentalSagaStateTest {
    @Test
    void rentFlowKeepsPendingParticipantWhenOtherParticipantFails() {
        RentalSagaState state = RentalSagaState.startRent("correlation-1", member(), item(), 10);

        state.recordParticipantResult("source-1", RentalSagaParticipant.BOOK, false);

        assertThat(state.bookResult()).isEqualTo(SagaParticipantStatus.FAILED);
        assertThat(state.memberResult()).isEqualTo(SagaParticipantStatus.PENDING);
        assertThat(state.sagaStatus()).isEqualTo(RentalSagaStatus.COMPENSATING);
    }

    @Test
    void lateParticipantSuccessAfterFailureIsTrackedForCompensation() {
        RentalSagaState state = RentalSagaState.startRent("correlation-1", member(), item(), 10);
        state.recordParticipantResult("source-1", RentalSagaParticipant.BOOK, false);

        state.recordParticipantResult("source-1", RentalSagaParticipant.MEMBER, true);

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
