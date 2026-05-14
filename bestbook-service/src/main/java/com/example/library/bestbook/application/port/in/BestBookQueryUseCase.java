package com.example.library.bestbook.application.port.in;

import com.example.library.bestbook.application.dto.BestBookResult;
import java.util.List;
import java.util.Optional;

/**
 * 인기 도서의 전체 목록과 단건 정보를 조회하는 application 계약.
 */
public interface BestBookQueryUseCase {
    /**
     * 모든 인기 도서 결과를 조회.
     *
     * @return 누적 대여 횟수가 기록된 인기 도서 결과 목록을 반환.
     */
    List<BestBookResult> getAllBooks();

    /**
     * 식별자로 인기 도서 결과를 조회.
     *
     * @param id 인기 도서 read model 식별자.
     * @return 조회 결과가 있으면 인기 도서 결과를 담은 Optional 을 반환.
     */
    Optional<BestBookResult> getBookById(Long id);
}
