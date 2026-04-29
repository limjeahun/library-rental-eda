package com.example.library.rental.application.inputport;

import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.application.usecase.InquiryUsecase;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InquiryInputPort implements InquiryUsecase {
    private final RentalCardOuputPort rentalCardOuputPort;

    public InquiryInputPort(RentalCardOuputPort rentalCardOuputPort) {
        this.rentalCardOuputPort = rentalCardOuputPort;
    }

    @Override
    public RentalCard getRentalCard(String userId) {
        return rentalCardOuputPort.loadRentalCard(userId)
            .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."));
    }

    @Override
    public List<RentItem> getRentItems(String userId) {
        return getRentalCard(userId).getRentItemList();
    }

    @Override
    public List<ReturnItem> getReturnItems(String userId) {
        return getRentalCard(userId).getReturnItemList();
    }
}
