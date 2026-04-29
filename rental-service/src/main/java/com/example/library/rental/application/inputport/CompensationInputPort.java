package com.example.library.rental.application.inputport;

import com.example.library.common.event.PointUseCommand;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.outputport.EventOuputPort;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.CompensationUsecase;
import com.example.library.rental.domain.model.RentalCard;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompensationInputPort implements CompensationUsecase {
    private static final long RENT_POINT = 10L;

    private final RentalCardOuputPort rentalCardOuputPort;
    private final EventOuputPort eventOuputPort;

    public CompensationInputPort(RentalCardOuputPort rentalCardOuputPort, EventOuputPort eventOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
        this.eventOuputPort = eventOuputPort;
    }

    @Override
    public void cancleRentItem(IDName idName, Item item) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancleRentItem(item);
        rentalCardOuputPort.save(rentalCard);
        eventOuputPort.occurPointUseCommand(createPointUseCommand(idName, RENT_POINT, "RENT_COMPENSATION"));
    }

    @Override
    public void cancleReturnItem(IDName idName, Item item, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancleReturnItem(item, point);
        rentalCardOuputPort.save(rentalCard);
        eventOuputPort.occurPointUseCommand(createPointUseCommand(idName, point, "RETURN_COMPENSATION"));
    }

    @Override
    public void cancleMakeAvailableRental(IDName idName, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancleMakeAvailableRental(point);
        rentalCardOuputPort.save(rentalCard);
    }

    private RentalCard load(IDName idName) {
        return rentalCardOuputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    private PointUseCommand createPointUseCommand(IDName idName, long point, String reason) {
        String eventId = UUID.randomUUID().toString();
        return new PointUseCommand(eventId, eventId, Instant.now(), idName, point, reason);
    }
}
