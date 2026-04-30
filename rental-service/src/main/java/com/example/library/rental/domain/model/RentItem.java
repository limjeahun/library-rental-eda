package com.example.library.rental.domain.model;

import com.example.library.common.vo.Item;
import java.time.LocalDate;
import java.util.Objects;

public class RentItem {
    private static final int RENTAL_DAYS = 14;

    private Item item;

    private LocalDate rentDate;
    private boolean overdued;
    private LocalDate overdueDate;

    public RentItem() {
    }

    public RentItem(Item item, LocalDate rentDate, boolean overdued, LocalDate overdueDate) {
        this.item = item;
        this.rentDate = rentDate;
        this.overdued = overdued;
        this.overdueDate = overdueDate;
    }

    public static RentItem createRentalItem(Item item) {
        LocalDate now = LocalDate.now();
        return new RentItem(item, now, false, now.plusDays(RENTAL_DAYS));
    }

    public boolean isSameItem(Item other) {
        return item != null && other != null && Objects.equals(item.getNo(), other.getNo());
    }

    public void markOverdued() {
        this.overdued = true;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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
