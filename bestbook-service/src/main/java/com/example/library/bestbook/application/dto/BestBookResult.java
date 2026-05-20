package com.example.library.bestbook.application.dto;

import com.example.library.bestbook.domain.model.BestBook;

/**
 * 인기 도서 read model을 외부 계층에 노출하기 위한 application 결과 DTO입니다.
 *
 * @param id 인기 도서 read model 식별자.
 * @param itemNo 인기 도서 집계 대상 도서 번호.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
 * @param rentCount 누적 대여 횟수.
 */
public record BestBookResult(Long id, Long itemNo, String itemTitle, long rentCount) {
    /**
     * 인기 도서 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param bestBook 변환할 인기 도서 도메인 모델.
     * @return 인기 도서 application 결과 DTO.
     */
    public static BestBookResult from(BestBook bestBook) {
        return new BestBookResult(
            bestBook.id(),
            bestBook.itemNo(),
            bestBook.itemTitle(),
            bestBook.rentCount()
        );
    }
}
