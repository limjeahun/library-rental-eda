package com.example.library.book.application.port.out;

import com.example.library.book.application.dto.BookRentalEventResult;

/**
 * 도서 서비스의 대여 이벤트 처리 결과를 발행하는 outbound port입니다.
 */
public interface PublishBookRentalResultPort {
    /**
     * 도서 서비스의 대여 이벤트 처리 결과를 발행합니다.
     *
     * @param result 도서 서비스의 이벤트 처리 application result입니다.
     */
    void publish(BookRentalEventResult result);
}
