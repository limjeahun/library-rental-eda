package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;

public interface OverdueItemUseCase {
    RentalCard overdueItem(IDName idName, Item item);
}
