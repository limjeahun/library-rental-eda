package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;

/**
 * 인기 도서 application 서비스가 read model을 저장하고 조회할 때 사용하는 MongoDB 저장소 계약입니다.
 */
public interface BestBookPort {
    /**
     * 모든 인기 도서 도메인 모델을 조회합니다.
     *
     * @return MongoDB read model에 저장된 인기 도서 목록을 반환합니다.
     */
    List<BestBook> findAll();

    /**
     * read model 식별자로 인기 도서 도메인 모델을 조회합니다.
     *
     * @param id 인기 도서 read model 식별자입니다.
     * @return 인기 도서 read model 식별자에 해당하는 결과를 담은 Optional을 반환합니다.
     */
    Optional<BestBook> findById(Long id);

    /**
     * 도서 번호로 인기 도서 도메인 모델을 조회합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @return 도서 번호에 해당하는 인기 도서 read model을 담은 Optional을 반환합니다.
     */
    Optional<BestBook> findByItemNo(Long itemNo);

    /**
     * 인기 도서 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    BestBook save(BestBook bestBook);
}
