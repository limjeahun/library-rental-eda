package com.example.library.member.application.port.out;

/**
 * Kafka 메시지 처리 완료 여부를 서비스 소유 저장소 기준으로 기록하는 outbound port입니다.
 */
public interface MessageIdempotencyPort {
    boolean markProcessed(String serviceName, String eventId, String correlationId, String messageType);
}
