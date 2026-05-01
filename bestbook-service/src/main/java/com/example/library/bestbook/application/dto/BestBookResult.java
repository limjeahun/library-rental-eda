package com.example.library.bestbook.application.dto;

import com.example.library.bestbook.domain.model.BestBook;

/**
 * 인기 도서 read model을 외부 계층에 노출하기 위한 application 결과 DTO입니다.
 *
 * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
 * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
 * @param rentCount 저장하거나 응답할 누적 대여 횟수입니다.
 */
public record BestBookResult(Long id, Long itemNo, String itemTitle, long rentCount) {
    /**
     * 인기 도서 도메인 모델을 application 결과 DTO로 변환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 도메인 모델 또는 application 결과 DTO를 HTTP 응답 DTO로 변환해 반환합니다.
     */
    public static BestBookResult from(BestBook bestBook) {
        return new BestBookResult(
            bestBook.getId(),
            bestBook.getItemNo(),
            bestBook.getItemTitle(),
            bestBook.getRentCount()
        );
    }
}
