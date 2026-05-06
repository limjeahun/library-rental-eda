package com.example.library.bestbook.application.port.in;

import com.example.library.bestbook.application.dto.CancelBestBookRentCommand;

/**
 * 보상 완료된 대여 이벤트를 인기 도서 read model에서 되돌리는 use case입니다.
 */
public interface CancelBestBookRentUseCase {
    void cancelRent(CancelBestBookRentCommand command);
}
