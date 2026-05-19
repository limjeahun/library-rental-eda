package com.example.library.member.application.dto;

public record MemberPointSaveResultContext(
        String sourceEventId,
        String correlationId,
        Long itemNo,
        String itemTitle
) {
    public static MemberPointSaveResultContext from(MemberPointSaveCommand command) {
        return new MemberPointSaveResultContext(
                command.eventId(),
                command.correlationId(),
                command.itemNo(),
                command.itemTitle()
        );
    }
}
