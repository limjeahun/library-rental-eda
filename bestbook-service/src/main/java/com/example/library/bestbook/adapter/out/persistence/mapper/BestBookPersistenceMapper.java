package com.example.library.bestbook.adapter.out.persistence.mapper;

import com.example.library.bestbook.adapter.out.persistence.document.BestBookDocument;
import com.example.library.bestbook.domain.model.BestBook;
import org.springframework.stereotype.Component;

/**
 * 인기 도서 도메인 모델과 MongoDB document 사이의 변환을 담당합니다.
 */
@Component
public class BestBookPersistenceMapper {
    /**
     * 인기 도서 도메인 모델을 MongoDB 저장용 document로 변환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return MongoDB 저장에 사용할 document 모델을 반환합니다.
     */
    public BestBookDocument toDocument(BestBook bestBook) {
        return new BestBookDocument(
            resolveDocumentId(bestBook),
            bestBook.getItemNo(),
            bestBook.getItemTitle(),
            bestBook.getRentCount()
        );
    }

    /**
     * MongoDB document를 인기 도서 도메인 모델로 복원합니다.
     *
     * @param document 도메인 모델로 변환할 MongoDB document입니다.
     * @return MongoDB document에서 복원한 인기 도서 도메인 모델을 반환합니다.
     */
    public BestBook toDomain(BestBookDocument document) {
        return new BestBook(
            document.getId(),
            document.getItemNo(),
            document.getItemTitle(),
            document.getRentCount()
        );
    }

    /**
     * 신규 read model은 도서 번호를 document 식별자로 사용해 같은 도서가 하나의 문서로 유지되게 합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 기존 document ID가 있으면 그 값을, 없으면 도서 번호를 반환합니다.
     */
    private Long resolveDocumentId(BestBook bestBook) {
        if (bestBook.getId() != null) {
            return bestBook.getId();
        }
        return bestBook.getItemNo();
    }
}
