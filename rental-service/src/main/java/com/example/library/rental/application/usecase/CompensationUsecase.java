package com.example.library.rental.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;

public interface CompensationUsecase {
    void cancleRentItem(IDName idName, Item item);

    void cancleReturnItem(IDName idName, Item item, long point);

    void cancleMakeAvailableRental(IDName idName, long point);
}
