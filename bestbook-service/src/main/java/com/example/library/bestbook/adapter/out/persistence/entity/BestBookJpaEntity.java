package com.example.library.bestbook.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "best_books")
public class BestBookJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long itemNo;

    @Column(nullable = false)
    private String itemTitle;

    @Column(nullable = false)
    private long rentCount;

    protected BestBookJpaEntity() {
    }

    public BestBookJpaEntity(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    public Long getId() {
        return id;
    }

    public Long getItemNo() {
        return itemNo;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public long getRentCount() {
        return rentCount;
    }
}
