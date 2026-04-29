package com.example.library.rental.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;

public interface RentItemUsecase {
    RentalCard rentItem(IDName idName, Item item);
}
