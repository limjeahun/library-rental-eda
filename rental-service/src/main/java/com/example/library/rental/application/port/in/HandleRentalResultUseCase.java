package com.example.library.rental.application.port.in;

import com.example.library.common.event.EventResult;

public interface HandleRentalResultUseCase {
    void handle(EventResult result);
}
