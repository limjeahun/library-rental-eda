package com.example.library.rental.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * 대여카드 엔티티 안에 포함되는 반납 완료 도서 JPA 값 객체입니다.
 */
@Embeddable
public class ReturnItemJpaEmbeddable {
    private Long itemNo;
    private String itemTitle;
    private LocalDate rentDate;
    private boolean overdue;
    private LocalDate overdueDate;
    private LocalDate returnDate;

    /**
     * JPA가 embeddable을 생성할 때 사용하는 기본 생성자입니다.
     */
    protected ReturnItemJpaEmbeddable() {
    }

    /**
     * 반납 항목의 현재 상태를 JPA 값 객체로 옮길 때 사용합니다.
     *
     * @param itemNo 도서 번호.
     * @param itemTitle 도서 제목.
     * @param rentDate 대여일.
     * @param overdue 연체 여부.
     * @param overdueDate 반납 예정일.
     * @param returnDate 실제 반납일.
     */
    public ReturnItemJpaEmbeddable(
        Long itemNo,
        String itemTitle,
        LocalDate rentDate,
        boolean overdue,
        LocalDate overdueDate,
        LocalDate returnDate
    ) {
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.overdue = overdue;
        this.overdueDate = overdueDate;
        this.returnDate = returnDate;
    }

    /**
     * 도서 번호를 반환합니다.
     *
     * @return 도서 번호를 반환합니다.
     */
    public Long getItemNo() {
        return itemNo;
    }

    /**
     * 도서 제목을 반환합니다.
     *
     * @return 도서 제목을 반환합니다.
     */
    public String getItemTitle() {
        return itemTitle;
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
     * 연체 여부를 반환합니다.
     *
     * @return 영속성 값에 연체 상태가 저장되어 있으면 true를 반환합니다.
     */
    public boolean isOverdue() {
        return overdue;
    }

    /**
     * 반납 예정일을 반환합니다.
     *
     * @return 반납 예정일을 반환합니다.
     */
    public LocalDate getOverdueDate() {
        return overdueDate;
    }

    /**
     * 실제 반납일을 반환합니다.
     *
     * @return 실제 반납일을 반환합니다.
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }
}
