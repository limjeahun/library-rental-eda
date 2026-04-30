package com.example.library.rental.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class ReturnItemJpaEmbeddable {
    private Long itemNo;
    private String itemTitle;
    private LocalDate rentDate;
    private boolean overdued;
    private LocalDate overdueDate;
    private LocalDate returnDate;

    protected ReturnItemJpaEmbeddable() {
    }

    public ReturnItemJpaEmbeddable(
        Long itemNo,
        String itemTitle,
        LocalDate rentDate,
        boolean overdued,
        LocalDate overdueDate,
        LocalDate returnDate
    ) {
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentDate = rentDate;
        this.overdued = overdued;
        this.overdueDate = overdueDate;
        this.returnDate = returnDate;
    }

    public Long getItemNo() {
        return itemNo;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public LocalDate getRentDate() {
        return rentDate;
    }

    public boolean isOverdued() {
        return overdued;
    }

    public LocalDate getOverdueDate() {
        return overdueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
