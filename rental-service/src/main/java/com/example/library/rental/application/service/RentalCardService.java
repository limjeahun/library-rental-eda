package com.example.library.rental.application.service;

import com.example.library.common.event.PointUseReason;
import com.example.library.rental.application.dto.ClearOverdueCommand;
import com.example.library.rental.application.dto.CreateRentalCardCommand;
import com.example.library.rental.application.dto.OverdueItemCommand;
import com.example.library.rental.application.dto.PointUseCommandPayload;
import com.example.library.rental.application.dto.RentItemCommand;
import com.example.library.rental.application.dto.RentItemResult;
import com.example.library.rental.application.dto.RentalCardResult;
import com.example.library.rental.application.dto.ReturnItemCommand;
import com.example.library.rental.application.dto.ReturnItemResult;
import com.example.library.rental.application.port.in.ClearOverdueItemUseCase;
import com.example.library.rental.application.port.in.CompensationUseCase;
import com.example.library.rental.application.port.in.CreateRentalCardUseCase;
import com.example.library.rental.application.port.in.OverdueItemUseCase;
import com.example.library.rental.application.port.in.RentItemUseCase;
import com.example.library.rental.application.port.in.RentalCardQueryUseCase;
import com.example.library.rental.application.port.in.ReturnItemUseCase;
import com.example.library.rental.application.port.out.CompensationIdempotencyPort;
import com.example.library.rental.application.port.out.LoadRentalCardPort;
import com.example.library.rental.application.port.out.PublishItemRentCanceledPort;
import com.example.library.rental.application.port.out.PublishItemRentedPort;
import com.example.library.rental.application.port.out.PublishItemReturnCanceledPort;
import com.example.library.rental.application.port.out.PublishItemReturnedPort;
import com.example.library.rental.application.port.out.PublishOverdueClearCanceledPort;
import com.example.library.rental.application.port.out.PublishOverdueClearedPort;
import com.example.library.rental.application.port.out.PublishPointUseCommandPort;
import com.example.library.rental.application.port.out.SaveRentalCardPort;
import com.example.library.rental.application.port.out.SaveRentalSagaStatePort;
import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemRentedDomainEvent;
import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemReturnedDomainEvent;
import com.example.library.rental.domain.event.OverdueClearCanceledDomainEvent;
import com.example.library.rental.domain.event.OverdueClearedDomainEvent;
import com.example.library.rental.domain.model.RentalCard;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.model.saga.RentalCompensationType;
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
 * 대여카드 생성, 도서 대여/반납, 연체료 정산, 보상 command 발행 흐름을 조율하는 service.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RentalCardService implements CreateRentalCardUseCase, RentItemUseCase, ReturnItemUseCase,
    OverdueItemUseCase, ClearOverdueItemUseCase, RentalCardQueryUseCase, CompensationUseCase {
    private final LoadRentalCardPort                loadRentalCardPort;
    private final SaveRentalCardPort                saveRentalCardPort;
    private final SaveRentalSagaStatePort           saveRentalSagaStatePort;
    private final PublishItemRentedPort             publishItemRentedPort;
    private final PublishItemReturnedPort           publishItemReturnedPort;
    private final PublishOverdueClearedPort         publishOverdueClearedPort;
    private final PublishPointUseCommandPort        publishPointUseCommandPort;
    private final PublishItemRentCanceledPort       publishItemRentCanceledPort;
    private final PublishItemReturnCanceledPort     publishItemReturnCanceledPort;
    private final PublishOverdueClearCanceledPort   publishOverdueClearCanceledPort;
    private final CompensationIdempotencyPort       compensationIdempotencyPort;

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

        long point = RentalPointPolicy.RENT.point();
        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(RentalSagaState.startRent(correlationId, member, item, point));
        publishItemRentedPort.publishRentalEvent(
            new ItemRentedDomainEvent(member, item, point),
            correlationId
        );
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

        long point = RentalPointPolicy.RETURN.point();
        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(RentalSagaState.startReturn(correlationId, member, item, point));
        publishItemReturnedPort.publishReturnEvent(
            new ItemReturnedDomainEvent(member, item, point),
            correlationId
        );
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

        String correlationId = UUID.randomUUID().toString();
        saveRentalSagaStatePort.save(RentalSagaState.startOverdue(correlationId, member, usedPoint));
        publishOverdueClearedPort.publishOverdueClearEvent(
            new OverdueClearedDomainEvent(member, usedPoint),
            correlationId
        );
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
     * 대여 참여 서비스 실패 결과에 대응해 대여를 취소하고 적립 포인트 차감 command를 발행합니다.
     *
     * @param member 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     */
    @Override
    public void cancelRentItem(RentalMember member, RentalItem item, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelRentItem(item);
        saveRentalCardPort.save(rentalCard);
        publishItemRentCanceledPort.publishRentCanceledEvent(
            new ItemRentCanceledDomainEvent(member, item, RentalPointPolicy.RENT.point()),
            correlationId
        );
    }

    @Override
    public void compensateRentPoint(RentalMember member, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RENT_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishPointUseCommand(
            createPointUseCommand(
                correlationId,
                member,
                RentalPointPolicy.RENT.point(),
                PointUseReason.RENT_COMPENSATION
            )
        );
    }

    /**
     * 반납 참여 서비스 실패 결과에 대응해 반납을 취소하고 적립 포인트 차감 command를 발행합니다.
     *
     * @param member 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     */
    @Override
    public void cancelReturnItem(RentalMember member, RentalItem item, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_CANCEL)) {
            return;
        }
        RentalCard rentalCard = load(member);
        rentalCard.cancelReturnItem(item, point);
        saveRentalCardPort.save(rentalCard);
        publishItemReturnCanceledPort.publishReturnCanceledEvent(
            new ItemReturnCanceledDomainEvent(member, item, point),
            correlationId
        );
    }

    @Override
    public void compensateReturnPoint(RentalMember member, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(correlationId, RentalCompensationType.RETURN_POINT_USE)) {
            return;
        }
        publishPointUseCommandPort.publishPointUseCommand(
            createPointUseCommand(correlationId, member, point, PointUseReason.RETURN_COMPENSATION)
        );
    }

    /**
     * 연체 해제 참여 서비스 실패 결과에 대응해 대여카드의 연체 상태를 복구.
     *
     * @param member 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     */
    @Override
    public void cancelMakeAvailableRental(RentalMember member, long point, String correlationId) {
        if (!compensationIdempotencyPort.markCompensated(
            correlationId,
            RentalCompensationType.OVERDUE_CLEAR_CANCEL
        )) {
            return;
        }

        RentalCard rentalCard = load(member);
        rentalCard.cancelMakeAvailableRental(point);
        saveRentalCardPort.save(rentalCard);

        publishOverdueClearCanceledPort.publishOverdueClearCanceledEvent(
            new OverdueClearCanceledDomainEvent(member, point),
            correlationId
        );
    }

    /**
     * 보상/변경 대상 대여카드를 조회하고 없으면 도메인 예외를 발생.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @return 보상 또는 상태 변경 대상 대여카드를 반환.
     */
    private RentalCard load(RentalMember idName) {
        return loadRentalCardPort.loadRentalCard(idName.id())
            .orElseThrow(() -> new IllegalArgumentException("대여카드가 없습니다."));
    }

    /**
     * 보상 흐름에서 회원 서비스에 포인트 차감을 요청하는 command 메시지를 생성.
     *
     * @param member 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값.
     * @param reason 실패 결과나 보상 command 의 사유.
     * @return 회원 서비스가 포인트를 차감할 수 있도록 회원 snapshot, 포인트, 사유를 담은 payload 를 반환.
     */
    private PointUseCommandPayload createPointUseCommand(
        String correlationId,
        RentalMember member,
        long point,
        PointUseReason reason
    ) {
        String commandCorrelationId = correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId;
        return new PointUseCommandPayload(commandCorrelationId, member.id(), member.name(), point, reason);
    }

    private RentalMember rentalMember(String userId, String userNm) {
        return new RentalMember(userId, userNm);
    }

    private RentalItem rentalItem(Long itemNo, String itemTitle) {
        return new RentalItem(itemNo, itemTitle);
    }

}
