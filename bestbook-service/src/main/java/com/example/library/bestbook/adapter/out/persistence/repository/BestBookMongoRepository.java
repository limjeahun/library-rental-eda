package com.example.library.bestbook.adapter.out.persistence.repository;

import com.example.library.bestbook.adapter.out.persistence.document.BestBookDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 인기 도서 MongoDB document를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface BestBookMongoRepository extends MongoRepository<BestBookDocument, Long> {
    /**
     * 도서 번호로 인기 도서 document를 조회합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @return 도서 번호에 해당하는 인기 도서 read model을 담은 Optional을 반환합니다.
     */
    Optional<BestBookDocument> findByItemNo(Long itemNo);
}
