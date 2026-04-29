package com.example.library.bestbook.framework.web.dto;

import com.example.library.bestbook.domain.model.BestBook;

public class BestBookOutPutDTO {
    private Long id;
    private Long itemNo;
    private String itemTitle;
    private long rentCount;

    public BestBookOutPutDTO() {
    }

    public BestBookOutPutDTO(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    public static BestBookOutPutDTO from(BestBook bestBook) {
        return new BestBookOutPutDTO(bestBook.getId(), bestBook.getItemNo(), bestBook.getItemTitle(), bestBook.getRentCount());
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
