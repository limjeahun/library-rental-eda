package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.RentalCardJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 대여카드 JPA 엔티티를 저장하고 조회하는 repository.
 */
public interface RentalCardJpaRepository extends JpaRepository<RentalCardJpaEntity, String> {
    /**
     * 회원 ID로 대여카드 엔티티를 조회.
     *
     * @param memberId 조회할 회원 로그인 ID.
     * @return 조회 결과가 있으면 도메인 모델을 담은 Optional 을, 없으면 빈 Optional 을 반환.
     */
    Optional<RentalCardJpaEntity> findByMemberId(String memberId);
}
