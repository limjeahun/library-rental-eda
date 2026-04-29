package com.example.library.bestbook.framework.web.dto;

import com.example.library.common.vo.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BestBookRegisterDTO {
    @NotNull
    private Long itemNo;

    @NotBlank
    private String itemTitle;

    public BestBookRegisterDTO() {
    }

    public Item toItem() {
        return new Item(itemNo, itemTitle);
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
}
