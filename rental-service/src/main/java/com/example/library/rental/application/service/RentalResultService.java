package com.example.library.rental.application.service;

import com.example.library.common.event.EventResult;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.HandleRentalResultUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RentalResultService implements HandleRentalResultUseCase {
    private static final Logger log = LoggerFactory.getLogger(RentalResultService.class);

    private final CompensationUseCase compensationUseCase;

    public RentalResultService(CompensationUseCase compensationUseCase) {
        this.compensationUseCase = compensationUseCase;
    }

    @Override
    public void handle(EventResult result) {
        if (result.isSuccessed()) {
            log.info("participant success eventType={} eventId={}", result.getEventType(), result.getEventId());
            return;
        }

        switch (result.getEventType()) {
            case RENT -> compensationUseCase.cancelRentItem(result.getIdName(), result.getItem());
            case RETURN -> compensationUseCase.cancelReturnItem(result.getIdName(), result.getItem(), result.getPoint());
            case OVERDUE -> compensationUseCase.cancelMakeAvailableRental(result.getIdName(), result.getPoint());
            default -> log.warn("unsupported eventType={}", result.getEventType());
        }
    }
}
