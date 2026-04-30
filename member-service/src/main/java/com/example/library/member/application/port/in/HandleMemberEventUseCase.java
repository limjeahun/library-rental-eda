package com.example.library.member.application.port.in;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;

public interface HandleMemberEventUseCase {
    void handleRent(ItemRented event);

    void handleReturn(ItemReturned event);

    void handleOverdueClear(OverdueCleared event);

    void handlePointUse(PointUseCommand command);
}
