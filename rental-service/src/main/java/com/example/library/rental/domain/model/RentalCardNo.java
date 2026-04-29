package com.example.library.rental.domain.model;

import jakarta.persistence.Embeddable;
import java.time.Year;
import java.util.UUID;

@Embeddable
public class RentalCardNo {
    private String no;

    public RentalCardNo() {
    }

    public RentalCardNo(String no) {
        this.no = no;
    }

    public static RentalCardNo createRentalCardNo() {
        return new RentalCardNo(Year.now().getValue() + "-" + UUID.randomUUID());
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }
}
