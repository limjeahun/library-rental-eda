package com.example.library.book.application.port.in;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;

public interface HandleBookRentalEventUseCase {
    void handleRent(ItemRented event);

    void handleReturn(ItemReturned event);
}
