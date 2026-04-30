package com.example.library.rental.application.service;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.port.in.ClearOverdueItemUseCase;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.CreateRentalCardUseCase;
import com.example.library.rental.application.port.in.OverdueItemUseCase;
import com.example.library.rental.application.port.in.RentItemUseCase;
import com.example.library.rental.application.port.in.RentalCardQueryUseCase;
import com.example.library.rental.application.port.in.ReturnItemUseCase;
import com.example.library.rental.application.port.out.RentalCardOutputPort;
import com.example.library.rental.application.port.out.RentalEventOutputPort;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RentalCardService implements CreateRentalCardUseCase, RentItemUseCase, ReturnItemUseCase,
    OverdueItemUseCase, ClearOverdueItemUseCase, RentalCardQueryUseCase, CompensationUseCase {
    private static final long RENT_POINT = 10L;
    private static final long RETURN_POINT = 10L;

    private final RentalCardOutputPort rentalCardOutputPort;
    private final RentalEventOutputPort rentalEventOutputPort;

    public RentalCardService(RentalCardOutputPort rentalCardOutputPort, RentalEventOutputPort rentalEventOutputPort) {
        this.rentalCardOutputPort = rentalCardOutputPort;
        this.rentalEventOutputPort = rentalEventOutputPort;
    }

    @Override
    public RentalCard createRentalCard(IDName creator) {
        return rentalCardOutputPort.loadRentalCard(creator.getId())
            .orElseGet(() -> rentalCardOutputPort.save(RentalCard.createRentalCard(creator)));
    }

    @Override
    public RentalCard rentItem(IDName idName, Item item) {
        RentalCard rentalCard = rentalCardOutputPort.loadRentalCard(idName.getId())
            .orElseGet(() -> RentalCard.createRentalCard(idName));
        rentalCard.rentItem(item);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        ItemRented itemRented = RentalCard.createItemRentedEvent(idName, item, RENT_POINT);
        rentalEventOutputPort.publishRentalEvent(itemRented);
        return saved;
    }

    @Override
    public RentalCard returnItem(IDName idName, Item item, LocalDate returnDate) {
        RentalCard rentalCard = load(idName);
        rentalCard.returnItem(item, returnDate);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        ItemReturned itemReturned = RentalCard.createItemReturnEvent(idName, item, RETURN_POINT);
        rentalEventOutputPort.publishReturnEvent(itemReturned);
        return saved;
    }

    @Override
    public RentalCard overdueItem(IDName idName, Item item) {
        RentalCard rentalCard = load(idName);
        rentalCard.overdueItem(item);
        return rentalCardOutputPort.save(rentalCard);
    }

    @Override
    public RentalCard clearOverdue(IDName idName, long point) {
        RentalCard rentalCard = load(idName);
        long usedPoint = rentalCard.makeAvailableRental(point);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        OverdueCleared overdueCleared = RentalCard.createOverdueClearedEvent(idName, usedPoint);
        rentalEventOutputPort.publishOverdueClearEvent(overdueCleared);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RentalCard getRentalCard(String userId) {
        return rentalCardOutputPort.loadRentalCard(userId)
            .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentItem> getRentItems(String userId) {
        return getRentalCard(userId).getRentItemList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnItem> getReturnItems(String userId) {
        return getRentalCard(userId).getReturnItemList();
    }

    @Override
    public void cancelRentItem(IDName idName, Item item) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelRentItem(item);
        rentalCardOutputPort.save(rentalCard);
        rentalEventOutputPort.publishPointUseCommand(createPointUseCommand(idName, RENT_POINT, "RENT_COMPENSATION"));
    }

    @Override
    public void cancelReturnItem(IDName idName, Item item, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelReturnItem(item, point);
        rentalCardOutputPort.save(rentalCard);
        rentalEventOutputPort.publishPointUseCommand(createPointUseCommand(idName, point, "RETURN_COMPENSATION"));
    }

    @Override
    public void cancelMakeAvailableRental(IDName idName, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelMakeAvailableRental(point);
        rentalCardOutputPort.save(rentalCard);
    }

    private RentalCard load(IDName idName) {
        return rentalCardOutputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    private PointUseCommand createPointUseCommand(IDName idName, long point, String reason) {
        String eventId = UUID.randomUUID().toString();
        return new PointUseCommand(eventId, eventId, Instant.now(), idName, point, reason);
    }
}
