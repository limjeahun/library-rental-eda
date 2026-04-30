package com.example.library.rental.domain.model;

import java.time.LocalDate;

public class ReturnItem {
    private RentItem item;

    private LocalDate returnDate;

    public ReturnItem() {
    }

    public ReturnItem(RentItem item, LocalDate returnDate) {
        this.item = item;
        this.returnDate = returnDate;
    }

    public static ReturnItem createReturnItem(RentItem item) {
        return new ReturnItem(item, LocalDate.now());
    }

    public static ReturnItem createReturnItem(RentItem item, LocalDate returnDate) {
        return new ReturnItem(item, returnDate);
    }

    public RentItem getItem() {
        return item;
    }

    public void setItem(RentItem item) {
        this.item = item;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
}
