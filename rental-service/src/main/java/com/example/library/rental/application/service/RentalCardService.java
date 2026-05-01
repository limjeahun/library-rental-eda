package com.example.library.rental.application.service;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.event.PointUseCommand;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.application.port.in.ClearOverdueItemUseCase;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.CreateRentalCardUseCase;
import com.example.library.rental.application.port.in.OverdueItemUseCase;
import com.example.library.rental.application.port.in.RentItemUseCase;
import com.example.library.rental.application.port.in.RentalCardQueryUseCase;
import com.example.library.rental.application.port.in.ReturnItemUseCase;
import com.example.library.rental.application.port.out.RentalCardOutputPort;
import com.example.library.rental.application.port.out.RentalEventOutputPort;
import com.example.library.rental.domain.model.RentItem;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.ReturnItem;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여카드 생성, 도서 대여/반납, 연체료 정산, 보상 command 발행 흐름을 조율하는 application service입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RentalCardService implements CreateRentalCardUseCase, RentItemUseCase, ReturnItemUseCase,
    OverdueItemUseCase, ClearOverdueItemUseCase, RentalCardQueryUseCase, CompensationUseCase {
    private static final long RENT_POINT = 10L;
    private static final long RETURN_POINT = 10L;

    private final RentalCardOutputPort rentalCardOutputPort;
    private final RentalEventOutputPort rentalEventOutputPort;

    /**
     * 회원에게 기존 대여카드가 있으면 반환하고 없으면 새 대여카드를 생성합니다.
     *
     * @param creator 대여카드를 생성할 회원의 식별 값입니다.
     * @return 기존 대여카드 또는 새로 저장한 대여카드를 반환합니다.
     */
    @Override
    public RentalCard createRentalCard(IDName creator) {
        return rentalCardOutputPort.loadRentalCard(creator.getId())
            .orElseGet(() -> rentalCardOutputPort.save(RentalCard.createRentalCard(creator)));
    }

    /**
     * 도서를 대여 처리하고 대여 완료 이벤트를 발행합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 도서가 대여 목록에 추가되고 대여 이벤트가 발행된 대여카드를 반환합니다.
     */
    @Override
    public RentalCard rentItem(IDName idName, Item item) {
        RentalCard rentalCard = rentalCardOutputPort.loadRentalCard(idName.getId())
            .orElseGet(() -> RentalCard.createRentalCard(idName));
        rentalCard.rentItem(item);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        ItemRented itemRented = RentalCard.createItemRentedEvent(idName, item, RENT_POINT);
        rentalEventOutputPort.publishRentalEvent(itemRented);
        return saved;
    }

    /**
     * 도서를 반납 처리하고 반납 완료 이벤트를 발행합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     * @return 도서가 반납 목록으로 이동하고 반납 이벤트가 발행된 대여카드를 반환합니다.
     */
    @Override
    public RentalCard returnItem(IDName idName, Item item, LocalDate returnDate) {
        RentalCard rentalCard = load(idName);
        rentalCard.returnItem(item, returnDate);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        ItemReturned itemReturned = RentalCard.createItemReturnEvent(idName, item, RETURN_POINT);
        rentalEventOutputPort.publishReturnEvent(itemReturned);
        return saved;
    }

    /**
     * 대여 중인 도서를 연체 처리합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대상 도서가 연체 표시되고 대여 정지 상태로 저장된 대여카드를 반환합니다.
     */
    @Override
    public RentalCard overdueItem(IDName idName, Item item) {
        RentalCard rentalCard = load(idName);
        rentalCard.overdueItem(item);
        return rentalCardOutputPort.save(rentalCard);
    }

    /**
     * 연체료를 포인트로 정산하고 연체 해제 이벤트를 발행합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 연체료가 포인트로 정산되고 연체 해제 이벤트가 발행된 대여카드를 반환합니다.
     */
    @Override
    public RentalCard clearOverdue(IDName idName, long point) {
        RentalCard rentalCard = load(idName);
        long usedPoint = rentalCard.makeAvailableRental(point);
        RentalCard saved = rentalCardOutputPort.save(rentalCard);
        OverdueCleared overdueCleared = RentalCard.createOverdueClearedEvent(idName, usedPoint);
        rentalEventOutputPort.publishOverdueClearEvent(overdueCleared);
        return saved;
    }

    /**
     * 회원 ID로 대여카드를 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원 ID에 해당하는 대여카드를 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public RentalCard getRentalCard(String userId) {
        return rentalCardOutputPort.loadRentalCard(userId)
            .orElseThrow(() -> new NoSuchElementException("대여카드를 찾을 수 없습니다."));
    }

    /**
     * 회원 ID로 현재 대여 중인 도서 목록을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원이 현재 대여 중인 도서 목록을 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RentItem> getRentItems(String userId) {
        return getRentalCard(userId).getRentItemList();
    }

    /**
     * 회원 ID로 반납 완료된 도서 목록을 조회합니다.
     *
     * @param userId 대여카드 소유자를 식별하는 회원 ID입니다.
     * @return 회원이 반납 완료한 도서 목록을 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReturnItem> getReturnItems(String userId) {
        return getRentalCard(userId).getReturnItemList();
    }

    /**
     * 대여 참여 서비스 실패 결과에 대응해 대여를 취소하고 적립 포인트 차감 command를 발행합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     */
    @Override
    public void cancelRentItem(IDName idName, Item item) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelRentItem(item);
        rentalCardOutputPort.save(rentalCard);
        rentalEventOutputPort.publishPointUseCommand(createPointUseCommand(idName, RENT_POINT, "RENT_COMPENSATION"));
    }

    /**
     * 반납 참여 서비스 실패 결과에 대응해 반납을 취소하고 적립 포인트 차감 command를 발행합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    @Override
    public void cancelReturnItem(IDName idName, Item item, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelReturnItem(item, point);
        rentalCardOutputPort.save(rentalCard);
        rentalEventOutputPort.publishPointUseCommand(createPointUseCommand(idName, point, "RETURN_COMPENSATION"));
    }

    /**
     * 연체 해제 참여 서비스 실패 결과에 대응해 대여카드의 연체 상태를 복구합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    @Override
    public void cancelMakeAvailableRental(IDName idName, long point) {
        RentalCard rentalCard = load(idName);
        rentalCard.cancelMakeAvailableRental(point);
        rentalCardOutputPort.save(rentalCard);
    }

    /**
     * 보상/변경 대상 대여카드를 조회하고 없으면 도메인 예외를 발생시킵니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @return 보상 또는 상태 변경 대상 대여카드를 반환합니다.
     */
    private RentalCard load(IDName idName) {
        return rentalCardOutputPort.loadRentalCard(idName.getId())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    /**
     * 보상 흐름에서 회원 서비스에 포인트 차감을 요청하는 command 메시지를 생성합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @param reason 실패 결과나 보상 command의 사유입니다.
     * @return 회원 서비스가 포인트를 차감할 수 있도록 회원, 포인트, 사유를 담은 PointUseCommand를 반환합니다.
     */
    private PointUseCommand createPointUseCommand(IDName idName, long point, String reason) {
        String eventId = UUID.randomUUID().toString();
        return new PointUseCommand(eventId, eventId, Instant.now(), idName, point, reason);
    }
}
