package com.example.library.bestbook.adapter.in.web.dto;

import com.example.library.bestbook.application.dto.BestBookResult;

/**
 * 인기 도서 API 응답으로 반환하는 HTTP DTO입니다.
 *
 * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
 * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
 * @param rentCount 저장하거나 응답할 누적 대여 횟수입니다.
 */
public record BestBookResponse(Long id, Long itemNo, String itemTitle, long rentCount) {
    /**
     * application 결과 DTO를 HTTP 응답 DTO로 변환합니다.
     *
     * @param result 처리하거나 발행할 result event 메시지입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    public static BestBookResponse from(BestBookResult result) {
        return new BestBookResponse(result.id(), result.itemNo(), result.itemTitle(), result.rentCount());
    }
}
