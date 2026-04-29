package com.example.library.rental.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;

@Embeddable
public class ReturnItem {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "item.no", column = @Column(name = "rent_item_no")),
        @AttributeOverride(name = "item.title", column = @Column(name = "rent_item_title")),
        @AttributeOverride(name = "rentDate", column = @Column(name = "rent_date")),
        @AttributeOverride(name = "overdued", column = @Column(name = "overdued")),
        @AttributeOverride(name = "overdueDate", column = @Column(name = "overdue_date"))
    })
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
