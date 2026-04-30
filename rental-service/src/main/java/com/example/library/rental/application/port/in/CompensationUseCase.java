package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;

public interface CompensationUseCase {
    void cancelRentItem(IDName idName, Item item);

    void cancelReturnItem(IDName idName, Item item, long point);

    void cancelMakeAvailableRental(IDName idName, long point);
}
