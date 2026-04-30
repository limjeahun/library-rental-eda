package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.RentalCardJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalCardJpaRepository extends JpaRepository<RentalCardJpaEntity, String> {
    Optional<RentalCardJpaEntity> findByMemberId(String memberId);
}
