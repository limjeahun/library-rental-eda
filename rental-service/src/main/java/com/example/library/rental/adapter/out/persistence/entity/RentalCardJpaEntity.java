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

    protected RentalCardJpaEntity() {
    }

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

    public String getRentalCardNo() {
        return rentalCardNo;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public RentStatus getRentStatus() {
        return rentStatus;
    }

    public long getLateFeePoint() {
        return lateFeePoint;
    }

    public List<RentItemJpaEmbeddable> getRentItems() {
        return rentItems;
    }

    public List<ReturnItemJpaEmbeddable> getReturnItems() {
        return returnItems;
    }
}
