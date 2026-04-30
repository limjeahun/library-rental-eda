package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;
import java.time.LocalDate;

public interface ReturnItemUseCase {
    RentalCard returnItem(IDName idName, Item item, LocalDate returnDate);
}
