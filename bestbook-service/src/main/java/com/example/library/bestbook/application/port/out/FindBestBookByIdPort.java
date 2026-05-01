package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;
import java.util.Optional;

/**
 * read model 식별자로 인기 도서 도메인 모델을 조회하는 outbound port입니다.
 */
public interface FindBestBookByIdPort {
    /**
     * read model 식별자로 인기 도서 도메인 모델을 조회합니다.
     *
     * @param id 인기 도서 read model 식별자입니다.
     * @return 인기 도서 read model 식별자에 해당하는 결과를 담은 Optional을 반환합니다.
     */
    Optional<BestBook> findById(Long id);
}
