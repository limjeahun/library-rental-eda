package com.example.library.bestbook.adapter.in.web.response;

import com.example.library.bestbook.application.dto.BestBookResult;

/**
 * 인기 도서 API 응답으로 반환하는 HTTP DTO.
 *
 * @param id 인기 도서 read model 식별자.
 * @param itemNo 인기 도서 집계 대상 도서 번호.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
 * @param rentCount 누적 대여 횟수.
 */
public record BestBookResponse(Long id, Long itemNo, String itemTitle, long rentCount) {
    /**
     * application 결과를 HTTP 응답 형태로 옮깁니다.
     *
     * @param result 인기 도서 application 결과 DTO.
     * @return 클라이언트에 반환할 HTTP 응답 DTO.
     */
    public static BestBookResponse from(BestBookResult result) {
        return new BestBookResponse(result.id(), result.itemNo(), result.itemTitle(), result.rentCount());
    }
}
