package com.example.library.rental.application.port.out;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;

/**
 * 대여 application 서비스가 대여/반납/연체해제 이벤트와 포인트 차감 command를 Kafka로 알릴 때 사용하는 발행 계약입니다.
 */
public interface RentalEventOutputPort {
    /**
     * 도서 대여 완료 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void publishRentalEvent(ItemRented event);

    /**
     * 도서 반납 완료 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void publishReturnEvent(ItemReturned event);

    /**
     * 연체 해제 완료 이벤트를 발행합니다.
     *
     * @param event 처리하거나 발행할 도메인 이벤트 메시지입니다.
     */
    void publishOverdueClearEvent(OverdueCleared event);

    /**
     * 보상 흐름에서 포인트 사용 command를 발행합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    void publishPointUseCommand(PointUseCommand command);
}
