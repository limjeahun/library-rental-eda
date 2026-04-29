package com.example.library.rental.application.inputport;

import com.example.library.common.event.OverdueCleared;
import com.example.library.common.vo.IDName;
import com.example.library.rental.application.outputport.EventOuputPort;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.ClearOverdueItemUsecase;
import com.example.library.rental.domain.model.RentalCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClearOverdueItemInputPort implements ClearOverdueItemUsecase {
    private final RentalCardOuputPort rentalCardOuputPort;
    private final EventOuputPort eventOuputPort;

    public ClearOverdueItemInputPort(RentalCardOuputPort rentalCardOuputPort, EventOuputPort eventOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
        this.eventOuputPort = eventOuputPort;
    }

    @Override
    public RentalCard clearOverdue(IDName idName, long point) {
        RentalCard rentalCard = rentalCardOuputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
        long usedPoint = rentalCard.makeAvailableRental(point);
        RentalCard saved = rentalCardOuputPort.save(rentalCard);
        OverdueCleared overdueCleared = RentalCard.createOverdueCleardEvent(idName, usedPoint);
        eventOuputPort.occurOverdueClearEvent(overdueCleared);
        return saved;
    }
}
