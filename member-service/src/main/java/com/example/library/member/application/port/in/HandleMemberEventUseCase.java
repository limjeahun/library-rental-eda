package com.example.library.member.application.port.in;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;

/**
 * 대여/반납 포인트 적립, 연체료 포인트 차감, 보상 포인트 차감 메시지를 처리하는 application 계약입니다.
 */
public interface HandleMemberEventUseCase {
    /**
     * 도서 대여 완료 이벤트를 처리합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void handleRent(ItemRented event);

    /**
     * 도서 반납 완료 이벤트를 처리합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void handleReturn(ItemReturned event);

    /**
     * 연체 해제 이벤트를 처리합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void handleOverdueClear(OverdueCleared event);

    /**
     * 포인트 사용 command를 처리합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    void handlePointUse(PointUseCommand command);
}
