package com.example.library.member.adapter.out.persistence.repository;

import com.example.library.member.adapter.out.persistence.entity.MemberJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    Optional<MemberJpaEntity> findByMemberId(String memberId);
}
