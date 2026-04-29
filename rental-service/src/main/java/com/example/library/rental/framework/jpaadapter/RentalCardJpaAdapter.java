package com.example.library.rental.framework.jpaadapter;

import com.example.library.rental.application.outputport.RentalCardOuputPort;
import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RentalCardJpaAdapter implements RentalCardOuputPort {
    private final RentalCardRepository rentalCardRepository;
    private final RentalCardQueryRepository rentalCardQueryRepository;

    public RentalCardJpaAdapter(RentalCardRepository rentalCardRepository, RentalCardQueryRepository rentalCardQueryRepository) {
        this.rentalCardRepository = rentalCardRepository;
        this.rentalCardQueryRepository = rentalCardQueryRepository;
    }

    @Override
    public Optional<RentalCard> loadRentalCard(String userId) {
        return rentalCardQueryRepository.findByMemberId(userId);
    }

    @Override
    public RentalCard save(RentalCard rentalCard) {
        return rentalCardRepository.save(rentalCard);
    }
}
