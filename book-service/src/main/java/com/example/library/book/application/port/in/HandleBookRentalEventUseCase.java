package com.example.library.book.application.port.in;

import com.example.library.book.application.dto.BookRentalCancelCommand;
import com.example.library.book.application.dto.BookRentalEventCommand;

/**
 * 대여 이벤트는 도서를 UNAVAILABLE 로, 반납 이벤트는 AVAILABLE 로 바꾸고 성공/실패 결과 이벤트를 만드는 application 계약입니다.
 */
public interface HandleBookRentalEventUseCase {
    /**
     * 도서 대여 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 대여 이벤트 application command입니다.
     */
    void handleRent(BookRentalEventCommand command);

    /**
     * 도서 반납 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 반납 이벤트 application command입니다.
     */
    void handleReturn(BookRentalEventCommand command);

    /**
     * 대여 보상 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 대여 보상 완료 application command입니다.
     */
    void handleRentCanceled(BookRentalCancelCommand command);

    /**
     * 반납 보상 완료 이벤트를 처리합니다.
     *
     * @param command 처리할 반납 보상 완료 application command입니다.
     */
    void handleReturnCanceled(BookRentalCancelCommand command);
}
