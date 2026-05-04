package com.example.library.rental.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.event.ItemRented;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.vo.LateFee;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class RentalCardTest {
    @Test
    void rentItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.rentItem(item(1));

        logRentalCard("rentItem", rentalCard);
        assertThat(rentalCard.getRentItemList()).hasSize(1);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
    }

    @Test
    void rentItemLimitIsFive() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        for (long i = 1; i <= 5; i++) {
            rentalCard.rentItem(item(i));
        }

        logRentalCard("rentItemLimitIsFive - before limit check", rentalCard);
        assertThatThrownBy(() -> rentalCard.rentItem(item(6)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateRentItemThrowsException() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);

        logRentalCard("duplicateRentItemThrowsException - before duplicate check", rentalCard);
        assertThatThrownBy(() -> rentalCard.rentItem(item))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);

        rentalCard.returnItem(item, LocalDate.now());

        logRentalCard("returnItem", rentalCard);
        assertThat(rentalCard.getRentItemList()).isEmpty();
        assertThat(rentalCard.getReturnItemList()).hasSize(1);
        assertThat(rentalCard.getLateFee().point()).isZero();
    }

    @Test
    void calculateLateFee() {
        Item item = item(1);
        RentalCard rentalCard = rentalCardWithRentItem(rentItem(item, LocalDate.now().minusDays(3)));

        rentalCard.returnItem(item, LocalDate.now());

        logRentalCard("calculateLateFee", rentalCard);
        assertThat(rentalCard.getLateFee().point()).isEqualTo(30);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    @Test
    void clearOverdue() {
        Item item = item(1);
        RentalCard rentalCard = rentalCardWithRentItem(rentItem(item, LocalDate.now().minusDays(2)));
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.makeAvailableRental(20);

        logRentalCard("clearOverdue", rentalCard);
        assertThat(rentalCard.getLateFee().point()).isZero();
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
    }

    @Test
    void cancelRentItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);

        rentalCard.cancelRentItem(item);

        logRentalCard("cancelRentItem", rentalCard);
        assertThat(rentalCard.getRentItemList()).isEmpty();
    }

    @Test
    void cancelReturnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.cancelReturnItem(item, 10);

        logRentalCard("cancelReturnItem", rentalCard);
        assertThat(rentalCard.getReturnItemList()).isEmpty();
        assertThat(rentalCard.getRentItemList()).hasSize(1);
    }

    @Test
    void cancelMakeAvailableRental() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.cancelMakeAvailableRental(40);

        logRentalCard("cancelMakeAvailableRental", rentalCard);
        assertThat(rentalCard.getLateFee().point()).isEqualTo(40);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    @Test
    void rentItemsAreNotExternallyMutable() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        rentalCard.rentItem(item(1));

        logRentalCard("rentItemsAreNotExternallyMutable - before mutation check", rentalCard);
        assertThatThrownBy(() -> rentalCard.getRentItemList().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createDomainEventKeepsCorrelationId() {
        String correlationId = "rent-flow-1";

        ItemRented event = RentalCard.createItemRentedEvent(correlationId, member(), item(1), 10);

        log.info(
            "createDomainEventKeepsCorrelationId eventId={} correlationId={} member={} item={} point={}",
            event.eventId(),
            event.correlationId(),
            event.idName(),
            event.item(),
            event.point()
        );
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventId()).isNotEqualTo(correlationId);
    }

    private void logRentalCard(String scenario, RentalCard rentalCard) {
        log.info(
            "{} rentalCardNo={} member={} status={} lateFee={} rentItemCount={} returnItemCount={} rentItems={} returnItems={}",
            scenario,
            rentalCard.getRentalCardNo(),
            rentalCard.getMember(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().point(),
            rentalCard.getRentItemList().size(),
            rentalCard.getReturnItemList().size(),
            rentalCard.getRentItemList(),
            rentalCard.getReturnItemList()
        );
    }

    private RentalCard rentalCardWithRentItem(RentItem rentItem) {
        return RentalCard.reconstitute(
            "card-1",
            member(),
            RentStatus.RENT_AVAILABLE,
            new LateFee(0),
            List.of(rentItem),
            List.of()
        );
    }

    private RentItem rentItem(Item item, LocalDate overdueDate) {
        return new RentItem(item, LocalDate.now().minusDays(10), false, overdueDate);
    }

    private IDName member() {
        return new IDName("jenny", "제니");
    }

    private Item item(long no) {
        return new Item(no, "book-" + no);
    }
}
