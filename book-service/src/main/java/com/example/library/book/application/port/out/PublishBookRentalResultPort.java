package com.example.library.book.application.port.out;

import com.example.library.book.application.dto.BookRentalEventCommand;
import com.example.library.book.domain.event.BookMadeAvailableDomainEvent;
import com.example.library.book.domain.event.BookMadeUnavailableDomainEvent;

/**
 * 도서 서비스의 대여 이벤트 처리 결과를 발행하는 outbound port입니다.
 */
public interface PublishBookRentalResultPort {
    /**
     * 대여 이벤트로 도서를 대여 불가능 상태로 변경한 성공 결과를 발행합니다.
     *
     * @param event 도서 aggregate 가 발생시킨 상태 변경 도메인 이벤트입니다.
     * @param sourceEventId 처리한 원본 통합 이벤트 ID입니다.
     * @param correlationId 비동기 흐름을 연결하는 correlation ID입니다.
     * @param memberId 회원 ID snapshot 입니다.
     * @param memberName 회원 이름 snapshot 입니다.
     * @param point 대여 흐름 포인트 snapshot 입니다.
     */
    void publishBookMadeUnavailable(
        BookMadeUnavailableDomainEvent event,
        String sourceEventId,
        String correlationId,
        String memberId,
        String memberName,
        long point
    );

    void publishBookMakeUnavailableFailed(BookRentalEventCommand command, String reason);

    void publishBookMadeAvailable(
        BookMadeAvailableDomainEvent event,
        String sourceEventId,
        String correlationId,
        String memberId,
        String memberName,
        long point
    );

    void publishBookMakeAvailableFailed(BookRentalEventCommand command, String reason);
}
