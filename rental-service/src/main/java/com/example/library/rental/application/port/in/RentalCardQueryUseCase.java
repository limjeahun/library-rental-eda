package com.example.library.rental.application.port.in;

import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.util.List;

public interface RentalCardQueryUseCase {
    RentalCard getRentalCard(String userId);

    List<RentItem> getRentItems(String userId);

    List<ReturnItem> getReturnItems(String userId);
}
