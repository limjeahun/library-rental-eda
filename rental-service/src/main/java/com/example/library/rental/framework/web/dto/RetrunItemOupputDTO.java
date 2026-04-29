package com.example.library.rental.framework.web.dto;

import com.example.library.rental.domain.model.ReturnItem;
import java.time.LocalDate;

public class RetrunItemOupputDTO {
    private Long itemId;
    private String itemTitle;
    private LocalDate rentDate;
    private LocalDate returnDate;

    public RetrunItemOupputDTO() {
    }

    public RetrunItemOupputDTO(Long itemId, String itemTitle, LocalDate rentDate, LocalDate returnDate) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.returnDate = returnDate;
    }

    public static RetrunItemOupputDTO from(ReturnItem returnItem) {
        return new RetrunItemOupputDTO(
            returnItem.getItem().getItem().getNo(),
            returnItem.getItem().getItem().getTitle(),
            returnItem.getItem().getRentDate(),
            returnItem.getReturnDate()
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

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
}
