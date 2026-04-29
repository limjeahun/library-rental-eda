package com.example.library.rental.application.inputport;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.OverdueItemUsercase;
import com.example.library.rental.domain.model.RentalCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OverDueItemInputPort implements OverdueItemUsercase {
    private final RentalCardOuputPort rentalCardOuputPort;

    public OverDueItemInputPort(RentalCardOuputPort rentalCardOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
    }

    @Override
    public RentalCard overdueItem(IDName idName, Item item) {
        RentalCard rentalCard = rentalCardOuputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
        rentalCard.overdueItem(item);
        return rentalCardOuputPort.save(rentalCard);
    }
}
