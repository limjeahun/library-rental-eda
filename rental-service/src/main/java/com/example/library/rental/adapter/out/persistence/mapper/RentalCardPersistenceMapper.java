package com.example.library.rental.adapter.out.persistence.mapper;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.adapter.out.persistence.entity.RentItemJpaEmbeddable;
import com.example.library.rental.adapter.out.persistence.entity.RentalCardJpaEntity;
import com.example.library.rental.adapter.out.persistence.entity.ReturnItemJpaEmbeddable;
import com.example.library.rental.domain.model.LateFee;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RentalCardPersistenceMapper {
    public RentalCardJpaEntity toJpaEntity(RentalCard rentalCard) {
        List<RentItemJpaEmbeddable> rentItems = rentalCard.getRentItemList().stream()
            .map(this::toRentItemJpa)
            .toList();
        List<ReturnItemJpaEmbeddable> returnItems = rentalCard.getReturnItemList().stream()
            .map(this::toReturnItemJpa)
            .toList();

        return new RentalCardJpaEntity(
            rentalCard.getRentalCardNo(),
            rentalCard.getMember().getId(),
            rentalCard.getMember().getName(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().getPoint(),
            rentItems,
            returnItems
        );
    }

    public RentalCard toDomain(RentalCardJpaEntity entity) {
        RentalCard rentalCard = new RentalCard(
            entity.getRentalCardNo(),
            new IDName(entity.getMemberId(), entity.getMemberName()),
            entity.getRentStatus(),
            new LateFee(entity.getLateFeePoint())
        );
        rentalCard.setRentItemList(entity.getRentItems().stream().map(this::toRentItemDomain).toList());
        rentalCard.setReturnItemList(entity.getReturnItems().stream().map(this::toReturnItemDomain).toList());
        return rentalCard;
    }

    private RentItemJpaEmbeddable toRentItemJpa(RentItem rentItem) {
        return new RentItemJpaEmbeddable(
            rentItem.getItem().getNo(),
            rentItem.getItem().getTitle(),
            rentItem.getRentDate(),
            rentItem.isOverdued(),
            rentItem.getOverdueDate()
        );
    }

    private ReturnItemJpaEmbeddable toReturnItemJpa(ReturnItem returnItem) {
        RentItem rentItem = returnItem.getItem();
        return new ReturnItemJpaEmbeddable(
            rentItem.getItem().getNo(),
            rentItem.getItem().getTitle(),
            rentItem.getRentDate(),
            rentItem.isOverdued(),
            rentItem.getOverdueDate(),
            returnItem.getReturnDate()
        );
    }

    private RentItem toRentItemDomain(RentItemJpaEmbeddable entity) {
        return new RentItem(
            new Item(entity.getItemNo(), entity.getItemTitle()),
            entity.getRentDate(),
            entity.isOverdued(),
            entity.getOverdueDate()
        );
    }

    private ReturnItem toReturnItemDomain(ReturnItemJpaEmbeddable entity) {
        RentItem rentItem = new RentItem(
            new Item(entity.getItemNo(), entity.getItemTitle()),
            entity.getRentDate(),
            entity.isOverdued(),
            entity.getOverdueDate()
        );
        return new ReturnItem(rentItem, entity.getReturnDate());
    }
}
