package com.example.library.rental.adapter.out.persistence;

import com.example.library.rental.adapter.out.persistence.mapper.RentalCardPersistenceMapper;
import com.example.library.rental.adapter.out.persistence.repository.RentalCardJpaRepository;
import com.example.library.rental.application.port.out.LoadRentalCardPort;
import com.example.library.rental.application.port.out.SaveRentalCardPort;
import com.example.library.rental.domain.model.RentalCard;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 대여카드 도메인 모델을 JPA 엔티티로 저장하고 회원 ID로 대여/반납 목록을 포함해 복원하는 영속성 컴포넌트.
 */
@Repository
@RequiredArgsConstructor
public class RentalCardPersistenceAdapter implements LoadRentalCardPort, SaveRentalCardPort {
    private final RentalCardJpaRepository repository;
    private final RentalCardPersistenceMapper mapper;

    /**
     * 대여카드 도메인 모델을 JPA 엔티티로 변환해 저장한 뒤 다시 도메인 모델로 반환.
     *
     * @param rentalCard 저장하거나 응답 DTO 로 변환할 대여카드 도메인 모델.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환.
     */
    @Override
    public RentalCard save(RentalCard rentalCard) {
        return mapper.toDomain(
                repository.save(mapper.toJpaEntity(rentalCard))
        );
    }

    /**
     * 회원 ID로 대여카드 엔티티를 조회하고 도메인 모델로 변환.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 회원 ID에 해당하는 대여카드를 담은 Optional 을 반환.
     */
    @Override
    public Optional<RentalCard> loadRentalCard(String userId) {
        return repository.findByMemberId(userId).map(mapper::toDomain);
    }
}
