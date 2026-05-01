package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;
import java.util.Optional;

/**
 * 도서 번호로 인기 도서 도메인 모델을 조회하는 outbound port입니다.
 */
public interface FindBestBookByItemNoPort {
    /**
     * 도서 번호로 인기 도서 도메인 모델을 조회합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @return 도서 번호에 해당하는 인기 도서 read model을 담은 Optional을 반환합니다.
     */
    Optional<BestBook> findByItemNo(Long itemNo);
}
