package com.example.library.bestbook.adapter.out.persistence.repository;

import com.example.library.bestbook.adapter.out.persistence.document.ProcessedMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 인기 도서 서비스 처리 완료 메시지 기록을 조회하고 저장하는 MongoDB repository입니다.
 */
public interface ProcessedMessageMongoRepository extends MongoRepository<ProcessedMessageDocument, String> {
    boolean existsByServiceNameAndEventId(String serviceName, String eventId);
}
