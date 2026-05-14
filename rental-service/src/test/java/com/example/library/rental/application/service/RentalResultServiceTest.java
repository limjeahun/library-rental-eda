package com.example.library.rental.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.common.event.EventType;
import com.example.library.common.event.InboundMessageType;
import com.example.library.common.event.Participant;
import com.example.library.common.event.PointUseReason;
import com.example.library.common.event.SagaStep;
import com.example.library.rental.application.dto.PointUseCommandPayload;
import com.example.library.rental.application.dto.RentalResultCommand;
import com.example.library.rental.application.port.out.CompensationIdempotencyPort;
import com.example.library.rental.application.port.out.LoadRentalCardPort;
import com.example.library.rental.application.port.out.LoadRentalSagaStatePort;
import com.example.library.rental.application.port.out.MessageIdempotencyPort;
import com.example.library.rental.application.port.out.PublishItemRentCanceledPort;
import com.example.library.rental.application.port.out.PublishItemReturnCanceledPort;
import com.example.library.rental.application.port.out.PublishOverdueClearCanceledPort;
import com.example.library.rental.application.port.out.PublishPointUseCommandPort;
import com.example.library.rental.application.port.out.SaveRentalCardPort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
import com.example.library.rental.domain.model.saga.RentalSagaParticipant;
import com.example.library.rental.domain.model.saga.RentalSagaState;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalResultServiceTest {
    private static final String CORRELATION_ID = "correlation-1";

    @Mock
    private MessageIdempotencyPort messageIdempotencyPort;

    @Mock
    private LoadRentalSagaStatePort loadRentalSagaStatePort;

    @Mock
    private SaveRentalSagaStatePort saveRentalSagaStatePort;

    @Mock
    private LoadRentalCardPort loadRentalCardPort;

    @Mock
    private SaveRentalCardPort saveRentalCardPort;

    @Mock
    private PublishPointUseCommandPort publishPointUseCommandPort;

    @Mock
    private PublishItemRentCanceledPort publishItemRentCanceledPort;

    @Mock
    private PublishItemReturnCanceledPort publishItemReturnCanceledPort;

    @Mock
    private PublishOverdueClearCanceledPort publishOverdueClearCanceledPort;

    @Mock
    private CompensationIdempotencyPort compensationIdempotencyPort;

    @InjectMocks
    private RentalResultService service;

    @Test
    void rentFailureCompensatesRentalCardAndRentPointDirectly() {
        RentalMember member = member();
        RentalItem item = item();
        RentalSagaState state = RentalSagaState.startRent(
            CORRELATION_ID,
            member,
            item,
            RentalPointPolicy.RENT.point()
        );
        state.recordParticipantResult("member-source", RentalSagaParticipant.MEMBER, true);

        RentalCard rentalCard = RentalCard.createRentalCard(member);
        rentalCard.rentItem(item);
        RentalResultCommand command = new RentalResultCommand(
            "result-1",
            CORRELATION_ID,
            "book-source",
            EventType.RENT,
            Participant.BOOK,
            SagaStep.BOOK_MAKE_UNAVAILABLE,
            false,
            member.id(),
            member.name(),
            item.no(),
            item.title(),
            RentalPointPolicy.RENT.point(),
            "book failed"
        );

        given(messageIdempotencyPort.markProcessed(
            command.eventId(),
            command.correlationId(),
            InboundMessageType.EVENT_RESULT
        )).willReturn(true);
        given(loadRentalSagaStatePort.loadByCorrelationId(CORRELATION_ID)).willReturn(Optional.of(state));
        given(compensationIdempotencyPort.markCompensated(CORRELATION_ID, RentalCompensationType.RENT_CANCEL))
            .willReturn(true);
        given(compensationIdempotencyPort.markCompensated(CORRELATION_ID, RentalCompensationType.RENT_POINT_USE))
            .willReturn(true);
        given(loadRentalCardPort.loadRentalCard(member.id())).willReturn(Optional.of(rentalCard));

        service.handle(command);

        ArgumentCaptor<RentalCard> cardCaptor = ArgumentCaptor.forClass(RentalCard.class);
        verify(saveRentalCardPort).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getRentItemList()).isEmpty();

        ArgumentCaptor<ItemRentCanceledDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(ItemRentCanceledDomainEvent.class);
        verify(publishItemRentCanceledPort).publishRentCanceledEvent(eventCaptor.capture(), eq(CORRELATION_ID));
        assertThat(eventCaptor.getValue().idName()).isEqualTo(member);
        assertThat(eventCaptor.getValue().item()).isEqualTo(item);
        assertThat(eventCaptor.getValue().point()).isEqualTo(RentalPointPolicy.RENT.point());

        ArgumentCaptor<PointUseCommandPayload> commandCaptor = ArgumentCaptor.forClass(PointUseCommandPayload.class);
        verify(publishPointUseCommandPort).publishPointUseCommand(commandCaptor.capture());
        assertThat(commandCaptor.getValue().correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(commandCaptor.getValue().memberId()).isEqualTo(member.id());
        assertThat(commandCaptor.getValue().memberName()).isEqualTo(member.name());
        assertThat(commandCaptor.getValue().point()).isEqualTo(RentalPointPolicy.RENT.point());
        assertThat(commandCaptor.getValue().reason()).isEqualTo(PointUseReason.RENT_COMPENSATION);
    }

    private RentalMember member() {
        return new RentalMember("member-1", "회원1");
    }

    private RentalItem item() {
        return new RentalItem(1L, "도서1");
    }
}
