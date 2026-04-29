package com.example.library.rental.framework.jpaadapter;

import com.example.library.rental.domain.model.QRentalCard;
import com.example.library.rental.domain.model.RentalCard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RentalCardQueryRepository {
    private final JPAQueryFactory queryFactory;

    public RentalCardQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<RentalCard> findByMemberId(String userId) {
        QRentalCard rentalCard = QRentalCard.rentalCard;
        return Optional.ofNullable(
            queryFactory.selectFrom(rentalCard)
                .where(rentalCard.member.id.eq(userId))
                .fetchFirst()
        );
    }
}
