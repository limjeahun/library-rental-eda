package com.example.library.rental.domain.event;

import java.time.Instant;

/**
 * rental-service 내부 aggregate 에서 발생한 service-local domain event 의 공통 marker interface.
 *
 * <p>Kafka 메시지 계약이 아니라 도메인 상태 변경으로 발생한 내부 사건을 표현한다.
 */
public interface RentalDomainEvent {
    Instant occurredAt();
}
