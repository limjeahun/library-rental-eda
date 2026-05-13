package com.example.library.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberEventServiceTest {
    @Mock
    private SavePointUseCase savePointUseCase;

    @Mock
    private UsePointUseCase usePointUseCase;

    @Mock
    private PublishMemberEventResultPort publishMemberEventResultPort;

    @Mock
    private MessageIdempotencyPort messageIdempotencyPort;

    @InjectMocks
    private MemberEventService service;

    @Test
    void overdueClearFailureIsPublishedWhenUsePointFails() {
        OverdueCleared event = overdueClearedEvent();
        given(messageIdempotencyPort.markProcessed(
            event.eventId(),
            event.correlationId(),
            InboundMessageType.OVERDUE_CLEARED
        )).willReturn(true);
        given(usePointUseCase.usePoint(any(ChangePointCommand.class)))
            .willThrow(new IllegalArgumentException("point use failed"));

        service.handleOverdueClear(event);

        EventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(event.eventId());
        assertThat(result.correlationId()).isEqualTo(event.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.OVERDUE);
        assertThat(result.participant()).isEqualTo(Participant.MEMBER);
        assertThat(result.step()).isEqualTo(SagaStep.MEMBER_USE_POINT);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("point use failed");
    }

    private EventResult publishedResult() {
        ArgumentCaptor<EventResult> captor = ArgumentCaptor.forClass(EventResult.class);
        verify(publishMemberEventResultPort).publish(captor.capture());
        return captor.getValue();
    }

    private OverdueCleared overdueClearedEvent() {
        return new OverdueCleared(
            "event-1",
            "correlation-1",
            Instant.parse("2026-05-13T00:00:00Z"),
            "member-1",
            "회원1",
            100L
        );
    }
}
