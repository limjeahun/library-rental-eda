package com.example.library.rental.adapter.out.persistence.mapper;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.adapter.out.persistence.entity.RentItemJpaEmbeddable;
import com.example.library.rental.adapter.out.persistence.entity.RentalCardJpaEntity;
import com.example.library.rental.adapter.out.persistence.entity.ReturnItemJpaEmbeddable;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.ReturnItem;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 대여카드 도메인 모델과 JPA 엔티티 그래프 사이의 변환을 담당.
 */
@Component
public class RentalCardPersistenceMapper {
    /**
     * 대여카드 도메인 모델을 JPA 저장용 엔티티로 변환.
     *
     * @param rentalCard 저장하거나 응답 DTO 로 변환할 대여카드 도메인 모델.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환.
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
            rentalCard.getMember().id(),
            rentalCard.getMember().name(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().point(),
            rentItems,
            returnItems
        );
    }

    /**
     * JPA 엔티티 그래프를 대여카드 도메인 모델로 복원.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티.
     * @return JPA 엔티티에서 복원한 대여카드 도메인 모델을 반환.
     */
    public RentalCard toDomain(RentalCardJpaEntity entity) {
        return RentalCard.reconstitute(
            entity.getRentalCardNo(),
            new IDName(entity.getMemberId(), entity.getMemberName()),
            entity.getRentStatus(),
            new LateFee(entity.getLateFeePoint()),
            entity.getRentItems().stream().map(this::toRentItemDomain).toList(),
            entity.getReturnItems().stream().map(this::toReturnItemDomain).toList()
        );
    }

    /**
     * 대여 항목 도메인 모델을 JPA embeddable 로 변환.
     *
     * @param rentItem 반납 또는 연체료 계산 대상 대여 항목.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환.
     */
    private RentItemJpaEmbeddable toRentItemJpa(RentItem rentItem) {
        return new RentItemJpaEmbeddable(
            rentItem.item().no(),
            rentItem.item().title(),
            rentItem.rentDate(),
            rentItem.overdued(),
            rentItem.overdueDate()
        );
    }

    /**
     * 반납 항목 도메인 모델을 JPA embeddable 로 변환.
     *
     * @param returnItem 변환할 반납 항목 도메인 모델.
     * @return 저장소 계층에서 사용할 JPA 모델을 반환.
     */
    private ReturnItemJpaEmbeddable toReturnItemJpa(ReturnItem returnItem) {
        RentItem rentItem = returnItem.item();
        return new ReturnItemJpaEmbeddable(
            rentItem.item().no(),
            rentItem.item().title(),
            rentItem.rentDate(),
            rentItem.overdued(),
            rentItem.overdueDate(),
            returnItem.returnDate()
        );
    }

    /**
     * 대여 항목 JPA embeddable 을 도메인 모델로 복원.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티.
     * @return JPA 임베디드 값에서 복원한 대여 항목을 반환.
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
     * 반납 항목 JPA embeddable 을 도메인 모델로 복원.
     *
     * @param entity 도메인 모델로 변환할 저장소 엔티티.
     * @return JPA 임베디드 값에서 복원한 반납 항목을 반환.
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
