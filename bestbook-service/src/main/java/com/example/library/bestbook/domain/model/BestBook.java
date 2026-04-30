package com.example.library.bestbook.domain.model;

import com.example.library.common.vo.Item;

public class BestBook {
    private Long id;
    private Long itemNo;
    private String itemTitle;
    private long rentCount;

    public BestBook(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    public static BestBook registerBestBook(Item item) {
        return new BestBook(null, item.getNo(), item.getTitle(), 1L);
    }

    public static BestBook registerBestBook(Long itemNo, String itemTitle) {
        return new BestBook(null, itemNo, itemTitle, 1L);
    }

    /**
     * @deprecated use {@link #increaseBestBookCount()}.
     */
    @Deprecated
    public long increseBestBookCount() {
        return increaseBestBookCount();
    }

    public long increaseBestBookCount() {
        this.rentCount += 1;
        return this.rentCount;
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
