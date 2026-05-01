package com.example.library.bestbook.application.port.in;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;

/**
 * 대여 이벤트의 도서 번호와 제목을 인기 도서 누적 대여 횟수에 반영하는 application 계약입니다.
 */
public interface RecordBestBookRentUseCase {
    /**
     * 도서 대여 정보를 인기 도서 read model에 반영합니다.
     *
     * @param command 집계할 도서 번호와 제목을 담은 인기 도서 기록 command입니다.
     */
    void recordRent(RecordBestBookRentCommand command);
}
