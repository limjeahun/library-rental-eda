package com.example.library.bestbook.adapter.in.web.dto;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 수동 테스트로 인기 도서 대여 기록을 추가하기 위한 HTTP 요청 DTO입니다.
 *
 * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
 * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
 */
public record BestBookRegisterRequest(
    @NotNull Long itemNo,
    @NotBlank String itemTitle
) {
    /**
     * web 요청 DTO를 인기 도서 기록 application command로 변환합니다.
     *
     * @return 요청 값을 업무 처리에 사용할 application command로 변환해 반환합니다.
     */
    public RecordBestBookRentCommand toCommand() {
        return new RecordBestBookRentCommand(itemNo, itemTitle);
    }
}
