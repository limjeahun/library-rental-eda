package com.example.library.rental.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.rental.adapter.out.persistence.repository.CompensationRecordJpaRepository;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CompensationRecordPersistenceAdapter.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mariadb://localhost:3306/rental_db",
    "spring.datasource.username=library",
    "spring.datasource.password=library",
    "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
class CompensationRecordPersistenceAdapterTest {
    private final CompensationRecordPersistenceAdapter adapter;
    private final CompensationRecordJpaRepository repository;

    @Autowired
    CompensationRecordPersistenceAdapterTest(
        CompensationRecordPersistenceAdapter adapter,
        CompensationRecordJpaRepository repository
    ) {
        this.adapter = adapter;
        this.repository = repository;
    }

    @Test
    void markCompensatedReturnsFalseForDuplicateWithoutBreakingTransaction() {
        String correlationId = "compensation-" + UUID.randomUUID();

        boolean first = adapter.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL);
        boolean duplicate = adapter.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL);
        boolean otherType = adapter.markCompensated(correlationId, RentalCompensationType.RENT_POINT_USE);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(otherType).isTrue();
        assertThat(repository.countByCorrelationId(correlationId)).isEqualTo(2);
    }
}
