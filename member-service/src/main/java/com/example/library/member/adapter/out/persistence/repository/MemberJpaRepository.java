package com.example.library.member.adapter.out.persistence.repository;

import com.example.library.member.adapter.out.persistence.entity.MemberJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 JPA 엔티티를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    /**
     * 회원 로그인 ID로 회원 엔티티를 조회합니다.
     *
     * @param memberId 조회할 회원 로그인 ID입니다.
     * @return 조회 결과가 있으면 도메인 모델을 담은 Optional을, 없으면 빈 Optional을 반환합니다.
     */
    Optional<MemberJpaEntity> findByMemberId(String memberId);
}
