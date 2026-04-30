package com.example.library.rental.application.port.out;

import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;

public interface RentalCardOutputPort {
    RentalCard save(RentalCard rentalCard);

    Optional<RentalCard> loadRentalCard(String userId);
}
