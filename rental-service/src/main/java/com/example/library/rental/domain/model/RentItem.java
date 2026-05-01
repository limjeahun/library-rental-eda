package com.example.library.rental.domain.model;

import com.example.library.common.vo.Item;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 대여 중인 단일 도서와 대여일, 연체 여부, 반납 예정일을 표현하는 도메인 모델입니다.
 */
public class RentItem {
    private static final int RENTAL_DAYS = 14;

    private Item item;

    private LocalDate rentDate;
    private boolean overdued;
    private LocalDate overdueDate;

    /**
     * 프레임워크 바인딩과 영속성 복원을 위한 기본 생성자입니다.
     */
    public RentItem() {
    }

    /**
     * 저장된 대여 항목 상태를 도메인 모델로 복원할 때 사용합니다.
     *
     * @param item 대여 항목으로 만들거나 비교할 도서 번호와 제목입니다.
     * @param rentDate 저장하거나 복원할 대여일입니다.
     * @param overdued 저장하거나 복원할 연체 여부입니다.
     * @param overdueDate 저장하거나 복원할 반납 예정일입니다.
     */
    public RentItem(Item item, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
        this.item = item;
        this.rentDate = rentDate;
        this.overdued = overdued;
        this.overdueDate = overdueDate;
    }

    /**
     * 오늘을 대여일로 하고 기본 대여 기간을 적용한 대여 항목을 생성합니다.
     *
     * @param item 대여 항목으로 만들거나 비교할 도서 번호와 제목입니다.
     * @return 오늘을 대여일로 하고 14일 뒤를 반납 예정일로 설정한 대여 항목을 반환합니다.
     */
    public static RentItem createRentalItem(Item item) {
        LocalDate now = LocalDate.now();
        return new RentItem(item, now, false, now.plusDays(RENTAL_DAYS));
    }

    /**
     * 도서 번호 기준으로 같은 대여 대상인지 비교합니다.
     *
     * @param other 같은 도서인지 비교할 대상 도서 값 객체입니다.
     * @return 현재 대여 항목과 비교 대상의 도서 번호가 같으면 true를 반환합니다.
     */
    public boolean isSameItem(Item other) {
        return item != null && other != null && Objects.equals(item.getNo(), other.getNo());
    }

    /**
     * 대여 항목을 연체 상태로 표시합니다.
     */
    public void markOverdued() {
        this.overdued = true;
    }

    /**
     * 대여된 도서 값을 반환합니다.
     *
     * @return 대여 중인 도서 번호와 제목을 반환합니다.
     */
    public Item getItem() {
        return item;
    }

    /**
     * 대여된 도서 값을 설정합니다.
     *
     * @param item 대여 항목으로 만들거나 비교할 도서 번호와 제목입니다.
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * 대여일을 반환합니다.
     *
     * @return 도서를 대여한 날짜를 반환합니다.
     */
    public LocalDate getRentDate() {
        return rentDate;
    }

    /**
     * 대여일을 설정합니다.
     *
     * @param rentDate 저장하거나 복원할 대여일입니다.
     */
    public void setRentDate(LocalDate rentDate) {
        this.rentDate = rentDate;
    }

    /**
     * 연체 여부를 반환합니다.
     *
     * @return 연체 상태로 표시된 대여 항목이면 true를 반환합니다.
     */
    public boolean isOverdued() {
        return overdued;
    }

    /**
     * 연체 여부를 설정합니다.
     *
     * @param overdued 저장하거나 복원할 연체 여부입니다.
     */
    public void setOverdued(boolean overdued) {
        this.overdued = overdued;
    }

    /**
     * 반납 예정일을 반환합니다.
     *
     * @return 대여일 기준 기본 대여 기간이 반영된 반납 예정일을 반환합니다.
     */
    public LocalDate getOverdueDate() {
        return overdueDate;
    }

    /**
     * 반납 예정일을 설정합니다.
     *
     * @param overdueDate 저장하거나 복원할 반납 예정일입니다.
     */
    public void setOverdueDate(LocalDate overdueDate) {
        this.overdueDate = overdueDate;
    }
}
