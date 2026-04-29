package com.example.library.rental.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.rental.domain.model.RentalCard;

public interface ClearOverdueItemUsecase {
    RentalCard clearOverdue(IDName idName, long point);
}
