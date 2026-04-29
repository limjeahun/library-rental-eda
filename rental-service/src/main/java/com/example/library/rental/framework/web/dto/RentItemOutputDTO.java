package com.example.library.rental.framework.web.dto;

import com.example.library.rental.domain.model.RentItem;
import java.time.LocalDate;

public class RentItemOutputDTO {
    private Long itemId;
    private String itemTitle;
    private LocalDate rentDate;
    private boolean overdued;
    private LocalDate overdueDate;

    public RentItemOutputDTO() {
    }

    public RentItemOutputDTO(Long itemId, String itemTitle, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.overdued = overdued;
        this.overdueDate = overdueDate;
    }

    public static RentItemOutputDTO from(RentItem rentItem) {
        return new RentItemOutputDTO(
            rentItem.getItem().getNo(),
            rentItem.getItem().getTitle(),
            rentItem.getRentDate(),
            rentItem.isOverdued(),
            rentItem.getOverdueDate()
        );
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public LocalDate getRentDate() {
        return rentDate;
    }

    public void setRentDate(LocalDate rentDate) {
        this.rentDate = rentDate;
    }

    public boolean isOverdued() {
        return overdued;
    }

    public void setOverdued(boolean overdued) {
        this.overdued = overdued;
    }

    public LocalDate getOverdueDate() {
        return overdueDate;
    }

    public void setOverdueDate(LocalDate overdueDate) {
        this.overdueDate = overdueDate;
    }
}
