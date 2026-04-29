package com.example.library.rental.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.CreateRentalCardUsecase;
import com.example.library.rental.domain.model.RentalCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRentalCardInputPort implements CreateRentalCardUsecase {
    private final RentalCardOuputPort rentalCardOuputPort;

    public CreateRentalCardInputPort(RentalCardOuputPort rentalCardOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
    }

    @Override
    public RentalCard createRentalCard(IDName creator) {
        return rentalCardOuputPort.loadRentalCard(creator.getId())
            .orElseGet(() -> rentalCardOuputPort.save(RentalCard.createRentalCard(creator)));
    }
}
