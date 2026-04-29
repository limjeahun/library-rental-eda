package com.example.library.rental.framework.web.dto;

public class RentalResultOuputDTO {
    private String message;
    private RentalCardOutputDTO rentalCard;

    public RentalResultOuputDTO() {
    }

    public RentalResultOuputDTO(String message, RentalCardOutputDTO rentalCard) {
        this.message = message;
        this.rentalCard = rentalCard;
    }

    public static RentalResultOuputDTO of(String message, RentalCardOutputDTO rentalCard) {
        return new RentalResultOuputDTO(message, rentalCard);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RentalCardOutputDTO getRentalCard() {
        return rentalCard;
    }

    public void setRentalCard(RentalCardOutputDTO rentalCard) {
        this.rentalCard = rentalCard;
    }
}
