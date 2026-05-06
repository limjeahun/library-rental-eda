package com.example.library.member.adapter.out.persistence.repository;

import com.example.library.member.adapter.out.persistence.entity.ProcessedMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 서비스 처리 완료 메시지 기록을 조회하고 저장하는 Spring Data repository입니다.
 */
public interface ProcessedMessageJpaRepository extends JpaRepository<ProcessedMessageJpaEntity, Long> {
    boolean existsByServiceNameAndEventId(String serviceName, String eventId);
}
