package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 대여 서비스가 도서 대여 완료 사실을 알리기 위해 발행하는 도메인 이벤트입니다.
 */
public class ItemRented {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private IDName idName;
    private Item item;
    private long point;

    /**
     * JSON 역직렬화를 위한 기본 생성자입니다.
     */
    public ItemRented() {
    }

    /**
     * 대여 이벤트 식별자, 상관관계 ID, 회원, 도서, 적립 포인트를 포함한 이벤트를 생성합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @param correlationId 비동기 command/event 흐름을 묶는 상관관계 식별자입니다.
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public ItemRented(String eventId, String correlationId, Instant occurredAt, IDName idName, Item item, long point) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.idName = idName;
        this.item = item;
        this.point = point;
    }

    /**
     * 이벤트 멱등성 판단에 사용하는 고유 이벤트 ID를 반환합니다.
     *
     * @return Kafka 중복 처리와 추적에 사용할 이벤트 ID를 반환합니다.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * JSON 역직렬화 시 이벤트 ID를 설정합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * 연관된 비동기 흐름을 추적하는 상관관계 ID를 반환합니다.
     *
     * @return 같은 대여 흐름의 메시지를 묶는 correlationId를 반환합니다.
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * JSON 역직렬화 시 상관관계 ID를 설정합니다.
     *
     * @param correlationId 비동기 command/event 흐름을 묶는 상관관계 식별자입니다.
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * 이벤트가 발생한 시각을 반환합니다.
     *
     * @return 이벤트 또는 command가 생성된 시각을 반환합니다.
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * JSON 역직렬화 시 이벤트 발생 시각을 설정합니다.
     *
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     */
    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * 이벤트 대상 회원 식별 값을 반환합니다.
     *
     * @return 메시지 대상 회원의 ID와 이름을 반환합니다.
     */
    public IDName getIdName() {
        return idName;
    }

    /**
     * JSON 역직렬화 시 회원 식별 값을 설정합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     */
    public void setIdName(IDName idName) {
        this.idName = idName;
    }

    /**
     * 대여된 도서 값을 반환합니다.
     *
     * @return 메시지 대상 도서의 번호와 제목을 반환합니다.
     */
    public Item getItem() {
        return item;
    }

    /**
     * JSON 역직렬화 시 도서 값을 설정합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * 대여 흐름에서 적용할 포인트 값을 반환합니다.
     *
     * @return 현재 저장된 포인트 금액을 반환합니다.
     */
    public long getPoint() {
        return point;
    }

    /**
     * JSON 역직렬화 시 포인트 값을 설정합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public void setPoint(long point) {
        this.point = point;
    }
}
