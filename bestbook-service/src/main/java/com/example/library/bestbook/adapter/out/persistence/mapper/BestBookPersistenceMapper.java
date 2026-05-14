package com.example.library.bestbook.adapter.out.persistence.mapper;

import com.example.library.bestbook.adapter.out.persistence.document.BestBookDocument;
import com.example.library.bestbook.domain.model.BestBook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 인기 도서 도메인 모델과 MongoDB document 사이의 변환을 담당합니다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BestBookPersistenceMapper {
    /**
     * 인기 도서 도메인 모델을 MongoDB 저장용 document로 변환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return MongoDB 저장에 사용할 document 모델을 반환합니다.
     */
    @Mapping(target = "id", expression = "java(resolveDocumentId(bestBook))")
    BestBookDocument toDocument(BestBook bestBook);

    /**
     * MongoDB document를 인기 도서 도메인 모델로 복원합니다.
     *
     * @param document 도메인 모델로 변환할 MongoDB document입니다.
     * @return MongoDB document에서 복원한 인기 도서 도메인 모델을 반환합니다.
     */
    BestBook toDomain(BestBookDocument document);

    /**
     * 신규 read model은 도서 번호를 document 식별자로 사용해 같은 도서가 하나의 문서로 유지되게 합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 기존 document ID가 있으면 그 값을, 없으면 도서 번호를 반환합니다.
     */
    default Long resolveDocumentId(BestBook bestBook) {
        if (bestBook.getId() != null) {
            return bestBook.getId();
        }
        return bestBook.getItemNo();
    }
}
