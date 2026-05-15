package com.example.library.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.common.event.InboundMessageType;
import com.example.library.member.application.dto.MemberOverdueClearCommand;
import com.example.library.member.application.dto.MemberPointSaveCommand;
import com.example.library.member.application.port.out.LoadMemberByIdNamePort;
import com.example.library.member.application.port.out.MessageIdempotencyPort;
import com.example.library.member.application.port.out.PublishMemberEventResultPort;
import com.example.library.member.application.port.out.SaveMemberPort;
import com.example.library.member.domain.event.MemberPointSavedDomainEvent;
import com.example.library.member.domain.event.MemberPointUsedDomainEvent;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import com.example.library.member.domain.vo.Authority;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.MemberIdentity;
import com.example.library.member.domain.vo.PassWord;
import com.example.library.member.domain.vo.Point;
import java.util.List;
import java.util.Optional;
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
    void rentSuccessPublishesAggregateDomainEvent() {
        MemberPointSaveCommand command = pointSaveCommand("event-1", "correlation-1");
        Member member = member(0);
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.ITEM_RENTED
        )).willReturn(true);
        given(loadMemberByIdNamePort.loadMemberByIdName(any()))
            .willReturn(Optional.of(member));

        service.handleRent(command);

        ArgumentCaptor<MemberPointSavedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(MemberPointSavedDomainEvent.class);
        verify(saveMemberPort).saveMember(member);
        verify(publishMemberEventResultPort).publishRentPointSaved(
            eventCaptor.capture(),
            eq(command.eventId()),
            eq(command.correlationId()),
            eq(command.itemNo()),
            eq(command.itemTitle())
        );
        MemberPointSavedDomainEvent event = eventCaptor.getValue();
        assertThat(event.member().id()).isEqualTo(command.memberId());
        assertThat(event.member().name()).isEqualTo(command.memberName());
        assertThat(event.point()).isEqualTo(command.point());
        assertThat(member.pullDomainEvents()).isEmpty();
    }

    @Test
    void overdueClearSuccessPublishesAggregateDomainEvent() {
        MemberOverdueClearCommand command = overdueClearCommand();
        Member member = member(200);
        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.OVERDUE_CLEARED
        )).willReturn(true);
        given(loadMemberByIdNamePort.loadMemberByIdName(any()))
            .willReturn(Optional.of(member));

        service.handleOverdueClear(command);

        ArgumentCaptor<MemberPointUsedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(MemberPointUsedDomainEvent.class);
        verify(saveMemberPort).saveMember(member);
        verify(publishMemberEventResultPort).publishOverduePointUsed(
            eventCaptor.capture(),
            eq(command.eventId()),
            eq(command.correlationId())
        );
        MemberPointUsedDomainEvent event = eventCaptor.getValue();
        assertThat(event.member().id()).isEqualTo(command.memberId());
        assertThat(event.member().name()).isEqualTo(command.memberName());
        assertThat(event.point()).isEqualTo(command.point());
        assertThat(member.pullDomainEvents()).isEmpty();
    }

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

        verify(publishMemberEventResultPort).publishOverduePointUseFailed(command, "point use failed");
    }

    private MemberPointSaveCommand pointSaveCommand(String eventId, String correlationId) {
        return new MemberPointSaveCommand(
            eventId,
            correlationId,
            "member-1",
            "회원1",
            1L,
            "도서1",
            10L
        );
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

    private Member member(long point) {
        return Member.reconstitute(
            1L,
            new MemberIdentity("member-1", "회원1"),
            new PassWord("1111"),
            new Email("member@example.com"),
            List.of(Authority.create(UserRole.USER)),
            new Point(point)
        );
    }
}
