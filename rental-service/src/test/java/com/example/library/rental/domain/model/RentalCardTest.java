package com.example.library.rental.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemRentedDomainEvent;
import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;
import com.example.library.rental.domain.event.OverdueClearCanceledDomainEvent;
import com.example.library.rental.domain.event.RentalDomainEvent;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
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
        assertThat(rentalCard.rentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
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
        RentalItem item = item(1);
        rentalCard.rentItem(item);

        logRentalCard("duplicateRentItemThrowsException - before duplicate check", rentalCard);
        assertThatThrownBy(() -> rentalCard.rentItem(item))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);

        rentalCard.returnItem(item, LocalDate.now());

        logRentalCard("returnItem", rentalCard);
        assertThat(rentalCard.getRentItemList()).isEmpty();
        assertThat(rentalCard.getReturnItemList()).hasSize(1);
        assertThat(rentalCard.lateFee().point()).isZero();
    }

    @Test
    void calculateLateFee() {
        RentalItem item = item(1);
        RentalCard rentalCard = rentalCardWithRentItem(rentItem(item, LocalDate.now().minusDays(3)));

        rentalCard.returnItem(item, LocalDate.now());

        logRentalCard("calculateLateFee", rentalCard);
        assertThat(rentalCard.lateFee().point()).isEqualTo(30);
        assertThat(rentalCard.rentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    @Test
    void clearOverdue() {
        RentalItem item = item(1);
        RentalCard rentalCard = rentalCardWithRentItem(rentItem(item, LocalDate.now().minusDays(2)));
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.makeAvailableRental(20);

        logRentalCard("clearOverdue", rentalCard);
        assertThat(rentalCard.lateFee().point()).isZero();
        assertThat(rentalCard.rentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
    }

    @Test
    void cancelRentItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);

        rentalCard.cancelRentItem(item);
        rentalCard.cancelRentItem(item);

        logRentalCard("cancelRentItem", rentalCard);
        assertThat(rentalCard.getRentItemList()).isEmpty();
    }

    @Test
    void cancelRentItemRegistersDomainEvent() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);
        rentalCard.pullDomainEvents();

        rentalCard.cancelRentItem(item);

        ItemRentCanceledDomainEvent event = pullSingleEvent(rentalCard, ItemRentCanceledDomainEvent.class);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.member()).isEqualTo(member());
        assertThat(event.item()).isEqualTo(item);
        assertThat(event.point()).isEqualTo(RentalPointPolicy.RENT.point());
    }

    @Test
    void cancelRentItemDoesNotRegisterDomainEventWhenAlreadyCanceled() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.cancelRentItem(item(1));

        assertThat(rentalCard.pullDomainEvents()).isEmpty();
    }

    @Test
    void cancelReturnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.cancelReturnItem(item, 10);
        rentalCard.cancelReturnItem(item, 10);

        logRentalCard("cancelReturnItem", rentalCard);
        assertThat(rentalCard.getReturnItemList()).isEmpty();
        assertThat(rentalCard.getRentItemList()).hasSize(1);
    }

    @Test
    void cancelReturnItemRegistersDomainEvent() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);
        rentalCard.returnItem(item, LocalDate.now());
        rentalCard.pullDomainEvents();

        rentalCard.cancelReturnItem(item, RentalPointPolicy.RETURN.point());

        ItemReturnCanceledDomainEvent event = pullSingleEvent(rentalCard, ItemReturnCanceledDomainEvent.class);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.member()).isEqualTo(member());
        assertThat(event.item()).isEqualTo(item);
        assertThat(event.point()).isEqualTo(RentalPointPolicy.RETURN.point());
    }

    @Test
    void cancelReturnItemDoesNotRegisterDomainEventWhenAlreadyCanceled() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        RentalItem item = item(1);
        rentalCard.rentItem(item);
        rentalCard.returnItem(item, LocalDate.now());
        rentalCard.cancelReturnItem(item, RentalPointPolicy.RETURN.point());
        rentalCard.pullDomainEvents();

        rentalCard.cancelReturnItem(item, RentalPointPolicy.RETURN.point());

        assertThat(rentalCard.pullDomainEvents()).isEmpty();
    }

    @Test
    void cancelMakeAvailableRental() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.cancelMakeAvailableRental(40);

        logRentalCard("cancelMakeAvailableRental", rentalCard);
        assertThat(rentalCard.lateFee().point()).isEqualTo(40);
        assertThat(rentalCard.rentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    @Test
    void cancelMakeAvailableRentalRegistersDomainEvent() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.cancelMakeAvailableRental(40);

        OverdueClearCanceledDomainEvent event = pullSingleEvent(
            rentalCard,
            OverdueClearCanceledDomainEvent.class
        );
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.member()).isEqualTo(member());
        assertThat(event.point()).isEqualTo(40);
    }

    @Test
    void cancelMakeAvailableRentalDoesNotRegisterDomainEventWhenAlreadyCanceled() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        rentalCard.cancelMakeAvailableRental(40);
        rentalCard.pullDomainEvents();

        rentalCard.cancelMakeAvailableRental(40);

        assertThat(rentalCard.pullDomainEvents()).isEmpty();
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
    void createDomainEventWithoutIntegrationMetadata() {
        ItemRentedDomainEvent event = ItemRentedDomainEvent.of(member(), item(1), 10);

        log.info(
            "createDomainEventWithoutIntegrationMetadata member={} RentalItem={} point={}",
            event.member(),
            event.item(),
            event.point()
        );
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.member()).isEqualTo(member());
        assertThat(event.item()).isEqualTo(item(1));
        assertThat(event.point()).isEqualTo(10);
    }

    private void logRentalCard(String scenario, RentalCard rentalCard) {
        log.info(
            "{} rentalCardNo={} member={} status={} lateFee={} rentItemCount={} returnItemCount={} rentItems={} returnItems={}",
            scenario,
            rentalCard.rentalCardNo(),
            rentalCard.member(),
            rentalCard.rentStatus(),
            rentalCard.lateFee().point(),
            rentalCard.getRentItemList().size(),
            rentalCard.getReturnItemList().size(),
            rentalCard.getRentItemList(),
            rentalCard.getReturnItemList()
        );
    }

    private <T extends RentalDomainEvent> T pullSingleEvent(RentalCard rentalCard, Class<T> eventType) {
        List<RentalDomainEvent> events = rentalCard.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(eventType);
        return eventType.cast(events.get(0));
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

    private RentItem rentItem(RentalItem item, LocalDate overdueDate) {
        return new RentItem(item, LocalDate.now().minusDays(10), false, overdueDate);
    }

    private RentalMember member() {
        return new RentalMember("jenny", "제니");
    }

    private RentalItem item(long no) {
        return new RentalItem(no, "book-" + no);
    }
}
