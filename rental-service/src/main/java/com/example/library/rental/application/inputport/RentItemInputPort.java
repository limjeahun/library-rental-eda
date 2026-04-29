package com.example.library.rental.application.inputport;

import com.example.library.common.event.ItemRented;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.outputport.EventOuputPort;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.RentItemUsecase;
import com.example.library.rental.domain.model.RentalCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RentItemInputPort implements RentItemUsecase {
    private static final long RENT_POINT = 10L;

    private final RentalCardOuputPort rentalCardOuputPort;
    private final EventOuputPort eventOuputPort;

    public RentItemInputPort(RentalCardOuputPort rentalCardOuputPort, EventOuputPort eventOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
        this.eventOuputPort = eventOuputPort;
    }

    @Override
    public RentalCard rentItem(IDName idName, Item item) {
        RentalCard rentalCard = rentalCardOuputPort.loadRentalCard(idName.getId())
            .orElseGet(() -> RentalCard.createRentalCard(idName));
        rentalCard.rentItem(item);
        RentalCard saved = rentalCardOuputPort.save(rentalCard);
        ItemRented itemRented = RentalCard.createItemRentedEvent(idName, item, RENT_POINT);
        eventOuputPort.occurRentalEvent(itemRented);
        return saved;
    }
}
