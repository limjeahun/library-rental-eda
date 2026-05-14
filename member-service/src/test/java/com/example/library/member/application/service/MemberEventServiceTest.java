package com.example.library.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.SagaStep;
import com.example.library.member.application.dto.MemberEventResult;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.port.out.LoadMemberByIdNamePort;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.application.port.out.SaveMemberPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberEventServiceTest {
    @Mock
    private LoadMemberByIdNamePort loadMemberByIdNamePort;

    @Mock
    private SaveMemberPort saveMemberPort;

    @Mock
    private PublishMemberEventResultPort publishMemberEventResultPort;

    @Mock
    private MessageIdempotencyPort messageIdempotencyPort;

    @InjectMocks
    private MemberEventService service;

    @Test
    void overdueClearFailureIsPublishedWhenUsePointFails() {
        MemberOverdueClearCommand command = overdueClearCommand();
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.OVERDUE_CLEARED
        )).willReturn(true);
        given(loadMemberByIdNamePort.loadMemberByIdName(any()))
            .willThrow(new IllegalArgumentException("point use failed"));

        service.handleOverdueClear(command);

        MemberEventResult result = publishedResult();
        assertThat(result.sourceEventId()).isEqualTo(command.eventId());
        assertThat(result.correlationId()).isEqualTo(command.correlationId());
        assertThat(result.eventType()).isEqualTo(EventType.OVERDUE);
        assertThat(result.participant()).isEqualTo(Participant.MEMBER);
        assertThat(result.step()).isEqualTo(SagaStep.MEMBER_USE_POINT);
        assertThat(result.successed()).isFalse();
        assertThat(result.reason()).isEqualTo("point use failed");
    }

    private MemberEventResult publishedResult() {
        ArgumentCaptor<MemberEventResult> captor = ArgumentCaptor.forClass(MemberEventResult.class);
        verify(publishMemberEventResultPort).publish(captor.capture());
        return captor.getValue();
    }

    private MemberOverdueClearCommand overdueClearCommand() {
        return new MemberOverdueClearCommand(
            "event-1",
            "correlation-1",
            "member-1",
            "회원1",
            100L
        );
    }
}
