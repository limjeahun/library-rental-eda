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
import java.util.ArrayList;
import java.util.List;

/**
 * 대여카드 도메인 모델을 MariaDB에 저장하기 위한 JPA 엔티티입니다.
 */
@Entity
@Table(name = "rental_cards")
public class RentalCardJpaEntity {
    @Id
    private String rentalCardNo;

    @Column(nullable = false, unique = true)
    private String memberId;

    @Column(nullable = false)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentStatus rentStatus;

    @Column(nullable = false)
    private long lateFeePoint;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_card_rent_items", joinColumns = @JoinColumn(name = "rental_card_no"))
    private List<RentItemJpaEmbeddable> rentItems = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_card_return_items", joinColumns = @JoinColumn(name = "rental_card_no"))
    private List<ReturnItemJpaEmbeddable> returnItems = new ArrayList<>();

    /**
     * JPA가 엔티티를 생성할 때 사용하는 기본 생성자입니다.
     */
    protected RentalCardJpaEntity() {
    }

    /**
     * 저장소 어댑터가 도메인 모델의 현재 상태를 JPA 엔티티로 옮길 때 사용합니다.
     *
     * @param rentalCardNo 설정할 대여카드 번호입니다.
     * @param memberId 조회할 회원 로그인 ID입니다.
     * @param memberName 저장할 회원 이름입니다.
     * @param rentStatus 저장하거나 설정할 대여카드 상태입니다.
     * @param lateFeePoint 저장하거나 복원할 연체료 포인트 값입니다.
     * @param rentItems 저장할 대여 중 도서 값 목록입니다.
     * @param returnItems 저장할 반납 완료 도서 값 목록입니다.
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

    /**
     * 대여카드 번호를 반환합니다.
     *
     * @return MariaDB에 저장된 대여카드 번호 문자열을 반환합니다.
     */
    public String getRentalCardNo() {
        return rentalCardNo;
    }

    /**
     * 회원 ID를 반환합니다.
     *
     * @return 대여카드 소유 회원 ID를 반환합니다.
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * 회원 이름을 반환합니다.
     *
     * @return 대여카드 소유 회원 이름을 반환합니다.
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * 대여 가능 상태를 반환합니다.
     *
     * @return 대여카드의 대여 가능 또는 정지 상태를 반환합니다.
     */
    public RentStatus getRentStatus() {
        return rentStatus;
    }

    /**
     * 연체료 포인트를 반환합니다.
     *
     * @return 대여카드에 누적된 연체료 포인트를 반환합니다.
     */
    public long getLateFeePoint() {
        return lateFeePoint;
    }

    /**
     * 저장된 대여 중 항목 목록을 반환합니다.
     *
     * @return 현재 대여 중인 도서 목록을 반환합니다.
     */
    public List<RentItemJpaEmbeddable> getRentItems() {
        return rentItems;
    }

    /**
     * 저장된 반납 완료 항목 목록을 반환합니다.
     *
     * @return 반납 완료된 도서 목록을 반환합니다.
     */
    public List<ReturnItemJpaEmbeddable> getReturnItems() {
        return returnItems;
    }
}
