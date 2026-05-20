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
     * 대여 항목의 현재 상태를 JPA 값 객체로 옮길 때 사용합니다.
     *
     * @param itemNo 도서 번호.
     * @param itemTitle 도서 제목.
     * @param rentDate 대여일.
     * @param overdue 연체 여부.
     * @param overdueDate 반납 예정일.
     */
    public RentItemJpaEmbeddable(Long itemNo, String itemTitle, LocalDate rentDate, boolean overdue, LocalDate overdueDate) {
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.overdue = overdue;
        this.overdueDate = overdueDate;
    }

}
