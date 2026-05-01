package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 대여 서비스가 도서 반납 완료 사실을 알리기 위해 발행하는 도메인 이벤트입니다.
 */
public class ItemReturned extends ItemRented {
    /**
     * JSON 역직렬화를 위한 기본 생성자입니다.
     */
    public ItemReturned() {
        super();
    }

    /**
     * 반납 이벤트 식별자, 상관관계 ID, 회원, 도서, 적립 포인트를 포함한 이벤트를 생성합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @param correlationId 비동기 command/event 흐름을 묶는 상관관계 식별자입니다.
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public ItemReturned(String eventId, String correlationId, Instant occurredAt, IDName idName, Item item, long point) {
        super(eventId, correlationId, occurredAt, idName, item, point);
    }

    /**
     * 대여 이벤트와 동일한 페이로드를 반납 이벤트 타입으로 변환합니다.
     *
     * @param itemRented 반납 이벤트로 변환할 대여 이벤트입니다.
     */
    public ItemReturned(ItemRented itemRented) {
        super(
            itemRented.getEventId(),
            itemRented.getCorrelationId(),
            itemRented.getOccurredAt(),
            itemRented.getIdName(),
            itemRented.getItem(),
            itemRented.getPoint()
        );
    }
}
