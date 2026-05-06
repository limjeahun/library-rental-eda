package com.example.library.bestbook.adapter.out.persistence.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 인기 도서 서비스가 이미 처리한 Kafka 메시지를 MongoDB에 기록하는 document입니다.
 */
@Document(collection = "processed_messages")
@CompoundIndex(
    name = "uk_bestbook_processed_message_service_event",
    def = "{'serviceName': 1, 'eventId': 1}",
    unique = true
)
public class ProcessedMessageDocument {
    @Id
    private String id;

    private String serviceName;
    private String eventId;
    private String correlationId;
    private String messageType;
    private Instant processedAt;

    protected ProcessedMessageDocument() {
    }

    public ProcessedMessageDocument(String serviceName, String eventId, String correlationId, String messageType) {
        this.serviceName = serviceName;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.messageType = messageType;
        this.processedAt = Instant.now();
    }
}
