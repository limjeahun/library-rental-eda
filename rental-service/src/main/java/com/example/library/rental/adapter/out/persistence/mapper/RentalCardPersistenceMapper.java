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

/**
 * 대여카드 도메인 모델과 JPA 엔티티 그래프 사이의 변환을 담당합니다.
 */
@Component
public class RentalCardPersistenceMapper {
    /**
     * 대여카드 도메인 모델을 JPA 저장용 엔티티로 변환합니다.
     *
     * @param rentalCard 저장하거나 응답 DTO로 변환할 대여카드 도메인 모델입니다.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환합니다.
     */
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

    /**
     * JPA 엔티티 그래프를 대여카드 도메인 모델로 복원합니다.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티입니다.
     * @return JPA 엔티티에서 복원한 대여카드 도메인 모델을 반환합니다.
     */
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

    /**
     * 대여 항목 도메인 모델을 JPA embeddable로 변환합니다.
     *
     * @param rentItem 반납 또는 연체료 계산 대상 대여 항목입니다.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환합니다.
     */
    private RentItemJpaEmbeddable toRentItemJpa(RentItem rentItem) {
        return new RentItemJpaEmbeddable(
            rentItem.getItem().getNo(),
            rentItem.getItem().getTitle(),
            rentItem.getRentDate(),
            rentItem.isOverdued(),
            rentItem.getOverdueDate()
        );
    }

    /**
     * 반납 항목 도메인 모델을 JPA embeddable로 변환합니다.
     *
     * @param returnItem 변환할 반납 항목 도메인 모델입니다.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환합니다.
     */
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

    /**
     * 대여 항목 JPA embeddable을 도메인 모델로 복원합니다.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티입니다.
     * @return JPA 임베디드 값에서 복원한 대여 항목을 반환합니다.
     */
    private RentItem toRentItemDomain(RentItemJpaEmbeddable entity) {
        return new RentItem(
            new Item(entity.getItemNo(), entity.getItemTitle()),
            entity.getRentDate(),
            entity.isOverdued(),
            entity.getOverdueDate()
        );
    }

    /**
     * 반납 항목 JPA embeddable을 도메인 모델로 복원합니다.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티입니다.
     * @return JPA 임베디드 값에서 복원한 반납 항목을 반환합니다.
     */
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
