package com.example.library.rental.application.port.in;

import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;

/**
 * 참여 서비스 실패 결과에 맞춰 대여, 반납, 연체 해제 상태와 포인트 흐름을 되돌리는 application 계약.
 */
public interface CompensationUseCase {
    /**
     * 대여 처리 실패 결과에 대응해 대여 내역을 취소.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param item 업무 대상 도서의 번호와 제목.
     */
    void cancelRentItem(RentalMember idName, RentalItem item, String correlationId);

    /**
     * 대여 포인트 적립 성공이 확인된 흐름에서 포인트 차감 보상 command 를 발행.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param correlationId 하나의 비동기 업무 흐름을 묶는 상관관계 식별자.
     */
    void compensateRentPoint(RentalMember idName, String correlationId);

    /**
     * 반납 처리 실패 결과에 대응해 반납 내역을 취소.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param item 업무 대상 도서의 번호와 제목.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     */
    void cancelReturnItem(RentalMember idName, RentalItem item, long point, String correlationId);

    /**
     * 반납 포인트 적립 성공이 확인된 흐름에서 포인트 차감 보상 command 를 발행.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     * @param correlationId 하나의 비동기 업무 흐름을 묶는 상관관계 식별자.
     */
    void compensateReturnPoint(RentalMember idName, long point, String correlationId);

    /**
     * 연체 해제 실패 결과에 대응해 정지 해제를 취소.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     */
    void cancelMakeAvailableRental(RentalMember idName, long point, String correlationId);
}
