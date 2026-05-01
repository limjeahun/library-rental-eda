package com.example.library.rental.adapter.out.persistence.repository;

import com.example.library.rental.adapter.out.persistence.entity.RentalCardJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 대여카드 JPA 엔티티를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface RentalCardJpaRepository extends JpaRepository<RentalCardJpaEntity, String> {
    /**
     * 회원 ID로 대여카드 엔티티를 조회합니다.
     *
     * @param memberId 조회할 회원 로그인 ID입니다.
     * @return 조회 결과가 있으면 도메인 모델을 담은 Optional을, 없으면 빈 Optional을 반환합니다.
     */
    Optional<RentalCardJpaEntity> findByMemberId(String memberId);
}
