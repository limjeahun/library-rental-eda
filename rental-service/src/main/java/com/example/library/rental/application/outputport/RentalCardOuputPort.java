package com.example.library.rental.application.outputport;

import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;

public interface RentalCardOuputPort {
    Optional<RentalCard> loadRentalCard(String userId);

    RentalCard save(RentalCard rentalCard);
}
