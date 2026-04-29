package com.example.library.rental.framework.jpaadapter;

import com.example.library.rental.domain.model.RentalCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalCardRepository extends JpaRepository<RentalCard, String> {
}
