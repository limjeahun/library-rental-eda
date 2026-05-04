package com.example.library.rental.adapter.out.persistence.entity;

import com.example.library.rental.domain.model.RentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 대여카드 엔티티.
 */
@Getter
@Entity
@Table(name = "rental_cards")
public class RentalCardJpaEntity {
    /**
     *  대여카드 번호를 반환.
     */
    @Id
    private String rentalCardNo;

    /**
     *  회원 ID를 반환.
     */
    @Column(nullable = false, unique = true)
    private String memberId;

    /**
     *  회원 이름을 반환.
     */
    @Column(nullable = false)
    private String memberName;

    /**
     *  대여 가능 상태를 반환.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentStatus rentStatus;

    /**
     *  연체료 포인트를 반환.
     */
    @Column(nullable = false)
    private long lateFeePoint;

    /**
     *  저장된 대여 중 항목 목록을 반환.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_card_rent_items", joinColumns = @JoinColumn(name = "rental_card_no"))
    private List<RentItemJpaEmbeddable> rentItems = new ArrayList<>();

    /**
     *  저장된 반납 완료 항목 목록을 반환.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_card_return_items", joinColumns = @JoinColumn(name = "rental_card_no"))
    private List<ReturnItemJpaEmbeddable> returnItems = new ArrayList<>();

    /**
     * JPA 가 엔티티를 생성할 때 사용하는 기본 생성자.
     */
    protected RentalCardJpaEntity() {}

    /**
     * 대여카드 엔티티 생성
     *
     * @param rentalCardNo 설정할 대여카드 번호.
     * @param memberId 조회할 회원 로그인 ID.
     * @param memberName 저장할 회원 이름.
     * @param rentStatus 저장하거나 설정할 대여카드 상태.
     * @param lateFeePoint 저장하거나 복원할 연체료 포인트 값.
     * @param rentItems 저장할 대여 중 도서 값 목록.
     * @param returnItems 저장할 반납 완료 도서 값 목록.
     */
    public RentalCardJpaEntity(
        String rentalCardNo,
        String memberId,
        String memberName,
        RentStatus rentStatus,
        long lateFeePoint,
        List<RentItemJpaEmbeddable> rentItems,
        List<ReturnItemJpaEmbeddable> returnItems
    ) {
        this.rentalCardNo = rentalCardNo;
        this.memberId = memberId;
        this.memberName = memberName;
        this.rentStatus = rentStatus;
        this.lateFeePoint = lateFeePoint;
        this.rentItems = new ArrayList<>(rentItems);
        this.returnItems = new ArrayList<>(returnItems);
    }

}
