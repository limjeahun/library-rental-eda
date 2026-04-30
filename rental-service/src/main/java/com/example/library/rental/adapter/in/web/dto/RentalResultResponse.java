package com.example.library.rental.adapter.in.web.dto;

public record RentalResultResponse(String message, RentalCardResponse rentalCard) {
    public static RentalResultResponse of(String message, RentalCardResponse rentalCard) {
        return new RentalResultResponse(message, rentalCard);
    }
}
