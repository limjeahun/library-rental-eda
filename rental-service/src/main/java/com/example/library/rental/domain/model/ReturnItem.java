package com.example.library.rental.domain.model;

import java.time.LocalDate;

/**
 * 반납 완료된 대여 항목과 실제 반납일을 표현하는 도메인 모델입니다.
 */
public class ReturnItem {
    private RentItem item;

    private LocalDate returnDate;

    /**
     * 프레임워크 바인딩과 영속성 복원을 위한 기본 생성자입니다.
     */
    public ReturnItem() {
    }

    /**
     * 대여 항목과 반납일로 반납 모델을 생성합니다.
     *
     * @param item 반납 처리로 이동할 대여 항목입니다.
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     */
    public ReturnItem(RentItem item, LocalDate returnDate) {
        this.item = item;
        this.returnDate = returnDate;
    }

    /**
     * 오늘을 반납일로 사용해 반납 모델을 생성합니다.
     *
     * @param item 반납 처리로 이동할 대여 항목입니다.
     * @return 반납된 대여 항목과 반납일을 담은 반납 모델을 반환합니다.
     */
    public static ReturnItem createReturnItem(RentItem item) {
        return new ReturnItem(item, LocalDate.now());
    }

    /**
     * 지정한 반납일로 반납 모델을 생성합니다.
     *
     * @param item 반납 처리로 이동할 대여 항목입니다.
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     * @return 반납된 대여 항목과 반납일을 담은 반납 모델을 반환합니다.
     */
    public static ReturnItem createReturnItem(RentItem item, LocalDate returnDate) {
        return new ReturnItem(item, returnDate);
    }

    /**
     * 반납된 대여 항목을 반환합니다.
     *
     * @return 반납된 대여 항목을 반환합니다.
     */
    public RentItem getItem() {
        return item;
    }

    /**
     * 반납된 대여 항목을 설정합니다.
     *
     * @param item 반납 처리로 이동할 대여 항목입니다.
     */
    public void setItem(RentItem item) {
        this.item = item;
    }

    /**
     * 실제 반납일을 반환합니다.
     *
     * @return 실제 반납일을 반환합니다.
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /**
     * 실제 반납일을 설정합니다.
     *
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     */
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
}
