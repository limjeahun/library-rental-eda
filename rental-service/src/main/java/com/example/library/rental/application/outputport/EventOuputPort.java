package com.example.library.rental.application.outputport;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;

public interface EventOuputPort {
    void occurRentalEvent(ItemRented itemRented);

    void occurRetunEvent(ItemReturned itemReturned);

    default void occurReturnEvent(ItemReturned itemReturned) {
        occurRetunEvent(itemReturned);
    }

    void occurOverdueClearEvent(OverdueCleared overdueCleared);

    void occurPointUseCommand(PointUseCommand pointUseCommand);
}
