package com.example.library.common.event;

import com.example.library.common.event.schema.EventResultMessage;
import com.example.library.common.event.schema.ItemRentCanceledMessage;
import com.example.library.common.event.schema.ItemRentedMessage;
import com.example.library.common.event.schema.ItemReturnCanceledMessage;
import com.example.library.common.event.schema.ItemReturnedMessage;
import com.example.library.common.event.schema.OverdueClearCanceledMessage;
import com.example.library.common.event.schema.OverdueClearedMessage;
import com.example.library.common.event.schema.PointUseCommandMessage;
import java.time.Instant;

/**
 * Java record 기반 통합 메시지와 Avro generated Kafka payload 사이의 변환을 담당합니다.
 */
public final class AvroMessageMapper {
    private AvroMessageMapper() {
    }

    public static ItemRented toItemRented(ItemRentedMessage message) {
        return new ItemRented(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getItemNo(),
            message.getItemTitle(),
            message.getPoint()
        );
    }

    public static ItemRentedMessage toItemRentedMessage(ItemRented event) {
        return ItemRentedMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setItemNo(event.itemNo())
            .setItemTitle(event.itemTitle())
            .setPoint(event.point())
            .build();
    }

    public static ItemReturned toItemReturned(ItemReturnedMessage message) {
        return new ItemReturned(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getItemNo(),
            message.getItemTitle(),
            message.getPoint()
        );
    }

    public static ItemReturnedMessage toItemReturnedMessage(ItemReturned event) {
        return ItemReturnedMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setItemNo(event.itemNo())
            .setItemTitle(event.itemTitle())
            .setPoint(event.point())
            .build();
    }

    public static OverdueCleared toOverdueCleared(OverdueClearedMessage message) {
        return new OverdueCleared(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getPoint()
        );
    }

    public static OverdueClearedMessage toOverdueClearedMessage(OverdueCleared event) {
        return OverdueClearedMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setPoint(event.point())
            .build();
    }

    public static PointUseCommand toPointUseCommand(PointUseCommandMessage message) {
        return new PointUseCommand(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getPoint(),
            PointUseReason.valueOf(message.getReason())
        );
    }

    public static PointUseCommandMessage toPointUseCommandMessage(PointUseCommand command) {
        return PointUseCommandMessage.newBuilder()
            .setEventId(command.eventId())
            .setCorrelationId(command.correlationId())
            .setOccurredAt(toEpochMillis(command.occurredAt()))
            .setMemberId(command.memberId())
            .setMemberName(command.memberName())
            .setPoint(command.point())
            .setReason(command.reason().name())
            .build();
    }

    public static EventResult toEventResult(EventResultMessage message) {
        return new EventResult(
            message.getEventId(),
            message.getCorrelationId(),
            message.getSourceEventId(),
            toInstant(message.getOccurredAt()),
            EventType.valueOf(message.getEventType()),
            Participant.valueOf(message.getParticipant()),
            SagaStep.valueOf(message.getStep()),
            message.getSuccessed(),
            message.getMemberId(),
            message.getMemberName(),
            message.getItemNo(),
            message.getItemTitle(),
            message.getPoint(),
            message.getReason()
        );
    }

    public static EventResultMessage toEventResultMessage(EventResult result) {
        return EventResultMessage.newBuilder()
            .setEventId(result.eventId())
            .setCorrelationId(result.correlationId())
            .setSourceEventId(result.sourceEventId())
            .setOccurredAt(toEpochMillis(result.occurredAt()))
            .setEventType(result.eventType().name())
            .setParticipant(result.participant().name())
            .setStep(result.step().name())
            .setSuccessed(result.successed())
            .setMemberId(result.memberId())
            .setMemberName(result.memberName())
            .setItemNo(result.itemNo())
            .setItemTitle(result.itemTitle())
            .setPoint(result.point())
            .setReason(result.reason())
            .build();
    }

    public static ItemRentCanceled toItemRentCanceled(ItemRentCanceledMessage message) {
        return new ItemRentCanceled(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getItemNo(),
            message.getItemTitle(),
            message.getPoint()
        );
    }

    public static ItemRentCanceledMessage toItemRentCanceledMessage(ItemRentCanceled event) {
        return ItemRentCanceledMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setItemNo(event.itemNo())
            .setItemTitle(event.itemTitle())
            .setPoint(event.point())
            .build();
    }

    public static ItemReturnCanceled toItemReturnCanceled(ItemReturnCanceledMessage message) {
        return new ItemReturnCanceled(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getItemNo(),
            message.getItemTitle(),
            message.getPoint()
        );
    }

    public static ItemReturnCanceledMessage toItemReturnCanceledMessage(ItemReturnCanceled event) {
        return ItemReturnCanceledMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setItemNo(event.itemNo())
            .setItemTitle(event.itemTitle())
            .setPoint(event.point())
            .build();
    }

    public static OverdueClearCanceled toOverdueClearCanceled(OverdueClearCanceledMessage message) {
        return new OverdueClearCanceled(
            message.getEventId(),
            message.getCorrelationId(),
            toInstant(message.getOccurredAt()),
            message.getMemberId(),
            message.getMemberName(),
            message.getPoint()
        );
    }

    public static OverdueClearCanceledMessage toOverdueClearCanceledMessage(OverdueClearCanceled event) {
        return OverdueClearCanceledMessage.newBuilder()
            .setEventId(event.eventId())
            .setCorrelationId(event.correlationId())
            .setOccurredAt(toEpochMillis(event.occurredAt()))
            .setMemberId(event.memberId())
            .setMemberName(event.memberName())
            .setPoint(event.point())
            .build();
    }

    private static Instant toInstant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    private static long toEpochMillis(Instant instant) {
        return instant.toEpochMilli();
    }
}
