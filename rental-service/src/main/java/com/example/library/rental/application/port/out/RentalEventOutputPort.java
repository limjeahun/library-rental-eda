package com.example.library.rental.application.port.out;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;

public interface RentalEventOutputPort {
    void publishRentalEvent(ItemRented event);

    void publishReturnEvent(ItemReturned event);

    void publishOverdueClearEvent(OverdueCleared event);

    void publishPointUseCommand(PointUseCommand command);
}
