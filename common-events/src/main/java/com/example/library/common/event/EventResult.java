package com.example.library.common.event;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;

/**
 * 참여 서비스가 Kafka command/event 처리 결과를 대여 서비스로 회신하는 결과 이벤트입니다.
 */
public class EventResult {
    private String eventId;
    private String correlationId;
    private Instant occurredAt;
    private EventType eventType;
    private boolean successed;
    private IDName idName;
    private Item item;
    private long point;
    private String reason;

    /**
     * JSON 역직렬화를 위한 기본 생성자입니다.
     */
    public EventResult() {
    }

    /**
     * 처리 대상 이벤트, 성공 여부, 보상에 필요한 회원/도서/포인트 정보를 포함한 결과 이벤트를 생성합니다.
     *
     * @param eventId 멱등성 판단과 추적에 사용할 이벤트 식별자입니다.
     * @param correlationId 비동기 command/event 흐름을 묶는 상관관계 식별자입니다.
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     * @param eventType 결과 이벤트가 대응하는 대여 흐름 타입입니다.
     * @param successed 참여 서비스 처리 성공 여부입니다.
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @param reason 실패 결과나 보상 command의 사유입니다.
     */
    public EventResult(
        String eventId,
        String correlationId,
        Instant occurredAt,
        EventType eventType,
        boolean successed,
        IDName idName,
        Item item,
        long point,
        String reason
    ) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.successed = successed;
        this.idName = idName;
        this.item = item;
        this.point = point;
        this.reason = reason;
    }

    /**
     * 원본 흐름의 고유 이벤트 ID를 반환합니다.
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
     * 비동기 처리 흐름을 묶는 상관관계 ID를 반환합니다.
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
     * 결과 이벤트가 발생한 시각을 반환합니다.
     *
     * @return 이벤트 또는 command가 생성된 시각을 반환합니다.
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * JSON 역직렬화 시 결과 발생 시각을 설정합니다.
     *
     * @param occurredAt 이벤트 또는 command가 발생한 시각입니다.
     */
    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * 결과가 대응하는 대여 이벤트 타입을 반환합니다.
     *
     * @return RENT, RETURN, OVERDUE 중 결과가 속한 대여 흐름 타입을 반환합니다.
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * JSON 역직렬화 시 대응 이벤트 타입을 설정합니다.
     *
     * @param eventType 결과 이벤트가 대응하는 대여 흐름 타입입니다.
     */
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * 참여 서비스 처리가 성공했는지 반환합니다.
     *
     * @return 참여 서비스가 성공 처리했으면 true, 실패 결과이면 false를 반환합니다.
     */
    public boolean isSuccessed() {
        return successed;
    }

    /**
     * JavaBean 호환성을 위해 성공 여부를 반환합니다.
     *
     * @return 참여 서비스가 성공 처리했으면 true, 실패 결과이면 false를 반환합니다.
     */
    public boolean getSuccessed() {
        return successed;
    }

    /**
     * JSON 역직렬화 시 성공 여부를 설정합니다.
     *
     * @param successed 참여 서비스 처리 성공 여부입니다.
     */
    public void setSuccessed(boolean successed) {
        this.successed = successed;
    }

    /**
     * 보상 또는 후속 처리 대상 회원 식별 값을 반환합니다.
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
     * 보상 또는 후속 처리 대상 도서 값을 반환합니다.
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
     * 보상 또는 후속 처리에 필요한 포인트 값을 반환합니다.
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

    /**
     * 실패 결과일 때 원인을 반환합니다.
     *
     * @return 실패 결과 또는 보상 command 사유를 반환합니다.
     */
    public String getReason() {
        return reason;
    }

    /**
     * JSON 역직렬화 시 실패 원인을 설정합니다.
     *
     * @param reason 실패 결과나 보상 command의 사유입니다.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
