package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;

/**
 * 모든 인기 도서 read model을 조회하는 outbound port입니다.
 */
public interface FindAllBestBooksPort {
    /**
     * 모든 인기 도서 도메인 모델을 조회합니다.
     *
     * @return MongoDB read model에 저장된 인기 도서 목록을 반환합니다.
     */
    List<BestBook> findAll();
}
