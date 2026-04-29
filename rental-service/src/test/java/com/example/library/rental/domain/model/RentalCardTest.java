package com.example.library.rental.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RentalCardTest {
    @Test
    void rentItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.rentItem(item(1));

        assertThat(rentalCard.getRentItemList()).hasSize(1);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
    }

    @Test
    void rentItemLimitIsFive() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        for (long i = 1; i <= 5; i++) {
            rentalCard.rentItem(item(i));
        }

        assertThatThrownBy(() -> rentalCard.rentItem(item(6)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);

        rentalCard.returnItem(item, LocalDate.now());

        assertThat(rentalCard.getRentItemList()).isEmpty();
        assertThat(rentalCard.getReturnItemList()).hasSize(1);
        assertThat(rentalCard.getLateFee().getPoint()).isZero();
    }

    @Test
    void calculateLateFee() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);
        rentalCard.getRentItemList().getFirst().setOverdueDate(LocalDate.now().minusDays(3));

        rentalCard.returnItem(item, LocalDate.now());

        assertThat(rentalCard.getLateFee().getPoint()).isEqualTo(30);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    @Test
    void clearOverdue() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);
        rentalCard.getRentItemList().getFirst().setOverdueDate(LocalDate.now().minusDays(2));
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.makeAvailableRental(20);

        assertThat(rentalCard.getLateFee().getPoint()).isZero();
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE);
    }

    @Test
    void cancleRentItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);

        rentalCard.cancleRentItem(item);

        assertThat(rentalCard.getRentItemList()).isEmpty();
    }

    @Test
    void cancleReturnItem() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());
        Item item = item(1);
        rentalCard.rentItem(item);
        rentalCard.returnItem(item, LocalDate.now());

        rentalCard.cancleReturnItem(item, 10);

        assertThat(rentalCard.getReturnItemList()).isEmpty();
        assertThat(rentalCard.getRentItemList()).hasSize(1);
    }

    @Test
    void cancleMakeAvailableRental() {
        RentalCard rentalCard = RentalCard.createRentalCard(member());

        rentalCard.cancleMakeAvailableRental(40);

        assertThat(rentalCard.getLateFee().getPoint()).isEqualTo(40);
        assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
    }

    private IDName member() {
        return new IDName("jenny", "제니");
    }

    private Item item(long no) {
        return new Item(no, "book-" + no);
    }
}
