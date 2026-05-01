package com.example.library.rental.application.port.out;

import com.example.library.common.event.PointUseCommand;

/**
 * 보상 흐름에서 포인트 사용 command를 발행하는 outbound port입니다.
 */
public interface PublishPointUseCommandPort {
    /**
     * 보상 흐름에서 포인트 사용 command를 발행합니다.
     *
     * @param command 포인트를 변경할 회원과 포인트 금액을 담은 command입니다.
     */
    void publishPointUseCommand(PointUseCommand command);
}
