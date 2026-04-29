package com.example.library.rental.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;
import java.time.LocalDate;

public interface ReturnItemUsercase {
    RentalCard returnItem(IDName idName, Item item, LocalDate returnDate);
}
