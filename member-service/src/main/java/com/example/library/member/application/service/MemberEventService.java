package com.example.library.member.application.service;

import com.example.library.common.event.EventResult;
import com.example.library.common.event.EventType;
import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.port.in.HandleMemberEventUseCase;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import com.example.library.member.application.port.out.MemberEventOutputPort;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MemberEventService implements HandleMemberEventUseCase {
    private static final Logger log = LoggerFactory.getLogger(MemberEventService.class);

    private final SavePointUseCase savePointUseCase;
    private final UsePointUseCase usePointUseCase;
    private final MemberEventOutputPort memberEventOutputPort;
    private final boolean forceOverdueClearFail;

    public MemberEventService(
        SavePointUseCase savePointUseCase,
        UsePointUseCase usePointUseCase,
        MemberEventOutputPort memberEventOutputPort,
        @Value("${app.failure.force-overdue-clear-fail:false}") boolean forceOverdueClearFail
    ) {
        this.savePointUseCase = savePointUseCase;
        this.usePointUseCase = usePointUseCase;
        this.memberEventOutputPort = memberEventOutputPort;
        this.forceOverdueClearFail = forceOverdueClearFail;
    }

    @Override
    public void handleRent(ItemRented event) {
        try {
            savePointUseCase.savePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
        } catch (Exception ex) {
            log.error("member rent point save failed eventId={}", event.getEventId(), ex);
        }
    }

    @Override
    public void handleReturn(ItemReturned event) {
        try {
            savePointUseCase.savePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
        } catch (Exception ex) {
            log.error("member return point save failed eventId={}", event.getEventId(), ex);
        }
    }

    @Override
    public void handleOverdueClear(OverdueCleared event) {
        try {
            if (forceOverdueClearFail) {
                throw new IllegalArgumentException("forced overdue_clear failure");
            }
            usePointUseCase.usePoint(new ChangePointCommand(event.getIdName(), event.getPoint()));
            memberEventOutputPort.publish(result(event, true, null));
        } catch (Exception ex) {
            log.error("overdue clear failed eventId={}", event.getEventId(), ex);
            memberEventOutputPort.publish(result(event, false, ex.getMessage()));
        }
    }

    @Override
    public void handlePointUse(PointUseCommand command) {
        try {
            usePointUseCase.usePoint(new ChangePointCommand(command.getIdName(), command.getPoint()));
        } catch (Exception ex) {
            log.error("point_use command failed eventId={} reason={}", command.getEventId(), command.getReason(), ex);
        }
    }

    private EventResult result(OverdueCleared event, boolean successed, String reason) {
        return new EventResult(
            event.getEventId(),
            event.getCorrelationId(),
            Instant.now(),
            EventType.OVERDUE,
            successed,
            event.getIdName(),
            null,
            event.getPoint(),
            reason
        );
    }
}
