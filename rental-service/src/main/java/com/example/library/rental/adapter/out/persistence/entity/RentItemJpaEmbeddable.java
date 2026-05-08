package com.example.library.rental.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 대여카드 엔티티 안에 포함되는 대여 중 도서 JPA 값 객체입니다.
 */
@Getter
@Embeddable
public class RentItemJpaEmbeddable {
    /**
     *  도서 번호를 반환합니다.
     */
    private Long itemNo;
    /**
     *  도서 제목을 반환합니다.
     */
    private String itemTitle;
    /**
     *  대여일을 반환합니다.
     */
    private LocalDate rentDate;
    /**
     *  연체 여부를 반환합니다.
     */
    private boolean overdue;
    /**
     *  반납 예정일을 반환합니다.
     */
    private LocalDate overdueDate;

    /**
     * JPA 가 embeddable 을 생성할 때 사용하는 기본 생성자입니다.
     */
    protected RentItemJpaEmbeddable() {
    }

    /**
     * 대여 중 도서의 저장 상태를 생성합니다.
     *
     * @param itemNo 대여 또는 반납 항목의 도서 번호입니다.
     * @param itemTitle 대여 또는 반납 항목의 도서 제목입니다.
     * @param rentDate 저장하거나 복원할 대여일입니다.
     * @param overdue 저장하거나 복원할 연체 여부입니다.
     * @param overdueDate 저장하거나 복원할 반납 예정일입니다.
     */
    public RentItemJpaEmbeddable(Long itemNo, String itemTitle, LocalDate rentDate, boolean overdue, LocalDate overdueDate) {
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.overdue = overdue;
        this.overdueDate = overdueDate;
    }

}
