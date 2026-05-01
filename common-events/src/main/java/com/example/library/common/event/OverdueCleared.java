package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import java.time.Instant;

/**
 * 대여 정지 해제를 위해 연체 포인트가 정산되었음을 알리는 도메인 이벤트입니다.
 */
public class OverdueCleared {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private IDName idName;
    private long point;

    /**
     * JSON 역직렬화를 위한 기본 생성자입니다.
     */
    public OverdueCleared() {
    }

    /**
     * 연체 해제 이벤트 식별자, 회원, 차감 포인트를 포함한 이벤트를 생성합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @param correlationId 비동기 command/event 흐름을 묶는 상관관계 식별자입니다.
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    public OverdueCleared(String eventId, String correlationId, Instant occurredAt, IDName idName, long point) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.idName = idName;
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
     * 연체 해제 대상 회원 식별 값을 반환합니다.
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
     * 연체 해제에 사용할 포인트 값을 반환합니다.
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
