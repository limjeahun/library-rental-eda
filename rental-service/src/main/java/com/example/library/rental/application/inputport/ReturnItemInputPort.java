package com.example.library.rental.application.inputport;

import com.example.library.common.event.ItemReturned;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.outputport.EventOuputPort;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.ReturnItemUsercase;
import com.example.library.rental.domain.model.RentalCard;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReturnItemInputPort implements ReturnItemUsercase {
    private static final long RETURN_POINT = 10L;

    private final RentalCardOuputPort rentalCardOuputPort;
    private final EventOuputPort eventOuputPort;

    public ReturnItemInputPort(RentalCardOuputPort rentalCardOuputPort, EventOuputPort eventOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
        this.eventOuputPort = eventOuputPort;
    }

    @Override
    public RentalCard returnItem(IDName idName, Item item, LocalDate returnDate) {
        RentalCard rentalCard = rentalCardOuputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
        rentalCard.returnItem(item, returnDate);
        RentalCard saved = rentalCardOuputPort.save(rentalCard);
        ItemReturned itemReturned = RentalCard.createItemReturnEvent(idName, item, RETURN_POINT);
        eventOuputPort.occurRetunEvent(itemReturned);
        return saved;
    }
}
