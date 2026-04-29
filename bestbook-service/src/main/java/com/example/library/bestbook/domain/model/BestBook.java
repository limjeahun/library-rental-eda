package com.example.library.bestbook.domain.model;

import com.example.library.common.vo.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "best_book")
public class BestBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long itemNo;

    private String itemTitle;

    private long rentCount;

    public BestBook() {
    }

    public BestBook(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    public static BestBook registerBestBook(Item item) {
        return new BestBook(null, item.getNo(), item.getTitle(), 1L);
    }

    public long increseBestBookCount() {
        this.rentCount += 1;
        return this.rentCount;
    }

    public long increaseBestBookCount() {
        return increseBestBookCount();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemNo() {
        return itemNo;
    }

    public void setItemNo(Long itemNo) {
        this.itemNo = itemNo;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public long getRentCount() {
        return rentCount;
    }

    public void setRentCount(long rentCount) {
        this.rentCount = rentCount;
    }
}
