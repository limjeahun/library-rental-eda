package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;

/**
 * 인기 도서 도메인 모델을 저장하는 outbound port입니다.
 */
public interface SaveBestBookPort {
    /**
     * 인기 도서 도메인 모델을 저장하고 저장된 모델을 반환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    BestBook save(BestBook bestBook);
}
