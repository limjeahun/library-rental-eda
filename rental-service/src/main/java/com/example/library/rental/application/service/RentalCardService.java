package com.example.library.rental.application.service;

import com.example.library.rental.application.dto.ClearOverdueCommand;
import com.example.library.rental.application.dto.CreateRentalCardCommand;
import com.example.library.rental.application.dto.OverdueItemCommand;
import com.example.library.rental.application.dto.RentItemCommand;
import com.example.library.rental.application.dto.RentItemResult;
import com.example.library.rental.application.dto.RentalCardResult;
import com.example.library.rental.application.dto.ReturnItemCommand;
import com.example.library.rental.application.dto.ReturnItemResult;
import com.example.library.rental.application.port.in.ClearOverdueItemUseCase;
import com.example.library.rental.application.port.in.CreateRentalCardUseCase;
import com.example.library.rental.application.port.in.OverdueItemUseCase;
import com.example.library.rental.application.port.in.RentItemUseCase;
import com.example.library.rental.application.port.in.RentalCardQueryUseCase;
import com.example.library.rental.application.port.in.ReturnItemUseCase;
import com.example.library.rental.application.port.out.LoadRentalCardPort;
import com.example.library.rental.application.port.out.PublishItemRentedPort;
import com.example.library.rental.application.port.out.PublishItemReturnedPort;
import com.example.library.rental.application.port.out.PublishOverdueClearedPort;
import com.example.library.rental.application.port.out.SaveRentalCardPort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.event.ItemRentedDomainEvent;
import com.example.library.rental.domain.event.ItemReturnedDomainEvent;
import com.example.library.rental.domain.event.OverdueClearedDomainEvent;
import com.example.library.rental.domain.event.RentalDomainEvent;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.model.saga.RentalSagaState;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.RentalMember;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여카드 생성, 도서 대여/반납, 연체료 정산 흐름을 조율하는 service.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RentalCardService implements CreateRentalCardUseCase, RentItemUseCase, ReturnItemUseCase,
    OverdueItemUseCase, ClearOverdueItemUseCase, RentalCardQueryUseCase {
    private final LoadRentalCardPort        loadRentalCardPort;
    private final SaveRentalCardPort        saveRentalCardPort;
    private final SaveRentalSagaStatePort   saveRentalSagaStatePort;
    private final PublishItemRentedPort     publishItemRentedPort;
    private final PublishItemReturnedPort   publishItemReturnedPort;
    private final PublishOverdueClearedPort publishOverdueClearedPort;

    /**
     * 회원에게 기존 대여카드가 있으면 반환하고 없으면 새 대여카드를 생성.
     *
     * @param command 대여카드를 생성할 회원 입력 command.
     * @return 기존 대여카드 또는 새로 저장한 대여카드를 반환.
     */
    @Override
    public RentalCardResult createRentalCard(CreateRentalCardCommand command) {
        var creator = new RentalMember(command.userId(), command.userNm());
        RentalCard rentalCard = loadRentalCardPort.loadRentalCard(creator.id())
            .orElseGet(
                    () -> saveRentalCardPort.save(
                            RentalCard.createRentalCard(creator)
                    )
            );
        return RentalCardResult.from(rentalCard);
    }

    /**
     * 도서를 대여 처리하고 대여 완료 이벤트를 발행.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 도서가 대여 목록에 추가되고 대여 이벤트가 발행된 대여카드를 반환.
     */
    @Override
    public RentalCardResult rentItem(RentItemCommand command) {
        var member = rentalMember(command.userId(), command.userNm());
        var item   = rentalItem(command.itemNo(), command.itemTitle());

        RentalCard rentalCard = loadRentalCardPort.loadRentalCard(member.id())
            .orElseGet(() -> RentalCard.createRentalCard(member));
        rentalCard.rentItem(item);
        RentalCard saved = saveRentalCardPort.save(rentalCard);

        var event = pullRequiredEvent(rentalCard, ItemRentedDomainEvent.class);
        // 이 correlationId로 book/member/bestbook 결과를 하나의 RENT 흐름으로 묶습니다.
        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(
                RentalSagaState.startRent(correlationId, event.member(), event.item(), event.point())
        );
        publishItemRentedPort.publishRentalEvent(event, correlationId);
        return RentalCardResult.from(saved);
    }

    /**
     * 도서를 반납 처리하고 반납 완료 이벤트를 발행.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 도서가 반납 목록으로 이동하고 반납 이벤트가 발행된 대여카드를 반환.
     */
    @Override
    public RentalCardResult returnItem(ReturnItemCommand command) {
        var member = rentalMember(command.userId(), command.userNm());
        var item   = rentalItem(command.itemNo(), command.itemTitle());

        LocalDate returnDate = LocalDate.now();
        RentalCard rentalCard = load(member);
        rentalCard.returnItem(item, returnDate);
        RentalCard saved = saveRentalCardPort.save(rentalCard);

        var event = pullRequiredEvent(rentalCard, ItemReturnedDomainEvent.class);
        // 이 correlationId로 book/member 결과를 하나의 RETURN 흐름으로 묶습니다.
        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(
                RentalSagaState.startReturn(correlationId, event.member(), event.item(), event.point())
        );
        publishItemReturnedPort.publishReturnEvent(event, correlationId);
        return RentalCardResult.from(saved);
    }

    /**
     * 대여 중인 도서를 연체 처리.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 대상 도서가 연체 표시되고 대여 정지 상태로 저장된 대여카드를 반환.
     */
    @Override
    public RentalCardResult overdueItem(OverdueItemCommand command) {
        var member = rentalMember(command.userId(), command.userNm());
        var item   = rentalItem(command.itemNo(), command.itemTitle());

        return RentalCardResult.from(
                saveRentalCardPort.save(
                        load(member).overdueItem(item)
                )
        );
    }

    /**
     * 연체료를 포인트로 정산하고 연체 해제 이벤트를 발행.
     *
     * @param command 대상 회원과 포인트 정보를 담은 입력 command.
     * @return 연체료가 포인트로 정산되고 연체 해제 이벤트가 발행된 대여카드를 반환.
     */
    @Override
    public RentalCardResult clearOverdue(ClearOverdueCommand command) {
        var member = rentalMember(command.userId(), command.userNm());
        long point = command.point();

        RentalCard rentalCard = load(member);
        long usedPoint = rentalCard.makeAvailableRental(point);
        RentalCard saved = saveRentalCardPort.save(rentalCard);

        var event = pullRequiredEvent(rentalCard, OverdueClearedDomainEvent.class);
        // 이 correlationId로 member 결과와 연체 해제 보상을 같은 흐름으로 묶습니다.
        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(
                RentalSagaState.startOverdue(correlationId, event.member(), usedPoint)
        );
        publishOverdueClearedPort.publishOverdueClearEvent(event, correlationId);
        return RentalCardResult.from(saved);
    }

    /**
     * 회원 ID로 대여카드를 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 회원 ID에 해당하는 대여카드를 반환.
     */
    @Override
    @Transactional(readOnly = true)
    public RentalCardResult getRentalCard(String userId) {
        return RentalCardResult.from(
                loadRentalCardPort.loadRentalCard(userId)
                        .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."))
        );
    }

    /**
     * 회원 ID로 현재 대여 중인 도서 목록을 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 회원이 현재 대여 중인 도서 목록을 반환.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RentItemResult> getRentItems(String userId) {
        return loadRentalCardPort.loadRentalCard(userId)
            .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."))
            .getRentItemList()
            .stream()
            .map(RentItemResult::from)
            .toList();
    }

    /**
     * 회원 ID로 반납 완료된 도서 목록을 조회.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID.
     * @return 회원이 반납 완료한 도서 목록을 반환.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReturnItemResult> getReturnItems(String userId) {
        return loadRentalCardPort.loadRentalCard(userId)
            .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."))
            .getReturnItemList()
            .stream()
            .map(ReturnItemResult::from)
            .toList();
    }

    /**
     * 상태 변경 대상 대여카드를 조회하고 없으면 도메인 예외를 발생.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @return 상태 변경 대상 대여카드를 반환.
     */
    private RentalCard load(RentalMember idName) {
        return loadRentalCardPort.loadRentalCard(idName.id())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    /**
     * 이번 상태 변경에서 필요한 도메인 이벤트를 꺼냅니다.
     *
     * <p>없으면 aggregate 상태 변경과 후속 발행 흐름이 어긋난 것으로 봅니다.
     */
    private <T extends RentalDomainEvent> T pullRequiredEvent(
            RentalCard rentalCard,
            Class<T> eventType
    ) {
        return rentalCard.pullDomainEvents()
                .stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Expected domain event was not raised: " + eventType.getSimpleName()
                ));
    }

    private RentalMember rentalMember(String userId, String userNm) {
        return new RentalMember(userId, userNm);
    }

    private RentalItem rentalItem(Long itemNo, String itemTitle) {
        return new RentalItem(itemNo, itemTitle);
    }

}
