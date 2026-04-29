package com.example.library.rental.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;

public interface OverdueItemUsercase {
    RentalCard overdueItem(IDName idName, Item item);
}
