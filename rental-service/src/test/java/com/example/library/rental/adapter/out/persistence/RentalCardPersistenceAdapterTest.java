package com.example.library.rental.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.rental.adapter.out.persistence.mapper.RentalCardPersistenceMapper;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RentalCardPersistenceAdapter.class, RentalCardPersistenceMapper.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mariadb://localhost:3306/rental_db",
    "spring.datasource.username=library",
    "spring.datasource.password=library",
    "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
class RentalCardPersistenceAdapterTest {
    private final RentalCardPersistenceAdapter adapter;
    private final EntityManager entityManager;

    @Autowired
    RentalCardPersistenceAdapterTest(RentalCardPersistenceAdapter adapter, EntityManager entityManager) {
        this.adapter = adapter;
        this.entityManager = entityManager;
    }

    @Test
    void saveAndLoadRentalCard() {
        LocalDate returnDate = LocalDate.now();
        RentalMember member = new RentalMember("db-member-" + UUID.randomUUID(), "DB 회원");
        RentalItem returnedBook = item(1);
        RentalItem stillRentedBook = item(2);
        RentalCard rentalCard = rentalCardWithRentItems(
            "card-db-" + UUID.randomUUID(),
            member,
            rentItem(returnedBook, returnDate.minusDays(2)),
            rentItem(stillRentedBook, returnDate.plusDays(10))
        );
        rentalCard.returnItem(returnedBook, returnDate);

        RentalCard saved = adapter.save(rentalCard);
        entityManager.flush();
        entityManager.clear();

        RentalCard loaded = adapter.loadRentalCard(member.id()).orElseThrow();

        logRentalCard("saved", saved);
        logRentalCard("loaded", loaded);
        assertThat(loaded.rentalCardNo()).isEqualTo(saved.rentalCardNo());
        assertThat(loaded.member()).isEqualTo(member);
        assertThat(loaded.rentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE);
        assertThat(loaded.lateFee().point()).isEqualTo(20);
        assertThat(loaded.getRentItemList())
            .singleElement()
            .satisfies(rentItem -> {
                assertThat(rentItem.item()).isEqualTo(stillRentedBook);
                assertThat(rentItem.overdue()).isFalse();
            });
        assertThat(loaded.getReturnItemList())
            .singleElement()
            .satisfies(returnItem -> {
                assertThat(returnItem.item().item()).isEqualTo(returnedBook);
                assertThat(returnItem.returnDate()).isEqualTo(returnDate);
            });
    }

    private RentalCard rentalCardWithRentItems(String rentalCardNo, RentalMember member, RentItem... rentItems) {
        return RentalCard.reconstitute(
            rentalCardNo,
            member,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0),
            List.of(rentItems),
            List.of()
        );
    }

    private RentItem rentItem(RentalItem item, LocalDate overdueDate) {
        return new RentItem(item, LocalDate.now().minusDays(10), false, overdueDate);
    }

    private RentalItem item(long no) {
        return new RentalItem(no, "book-" + no);
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
}
