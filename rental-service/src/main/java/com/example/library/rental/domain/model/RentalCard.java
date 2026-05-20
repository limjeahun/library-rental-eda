package com.example.library.rental.domain.model;

import com.example.library.rental.domain.event.ItemRentCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemRentedDomainEvent;
import com.example.library.rental.domain.event.ItemReturnCanceledDomainEvent;
import com.example.library.rental.domain.event.ItemReturnedDomainEvent;
import com.example.library.rental.domain.event.OverdueClearCanceledDomainEvent;
import com.example.library.rental.domain.event.OverdueClearedDomainEvent;
import com.example.library.rental.domain.event.RentalDomainEvent;
import com.example.library.rental.domain.model.policy.RentalLateFeePolicy;
import com.example.library.rental.domain.model.policy.RentalLimitPolicy;
import com.example.library.rental.domain.model.policy.RentalPointPolicy;
import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.vo.RentalCardNo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 회원의 대여 상태와 대여/반납/연체 목록을 관리하는 aggregate root입니다.
 *
 * <p>상태 변경은 행위 메서드만 허용하고, 변경 후 service-local domain event를 기록합니다.
 * 기술 관심사와 Lombok accessor는 사용하지 않습니다.
 */
public class RentalCard {
    private final String rentalCardNo;
    private final RentalMember member;
    private RentStatus rentStatus;
    private LateFee lateFee;
    private final List<RentItem> rentItemList;
    private final List<ReturnItem> returnItemList;
    private final List<RentalDomainEvent> domainEvents = new ArrayList<>();

    /**
     * 신규 대여카드 factory에서만 사용하는 생성자입니다.
     *
     * @param rentalCardNo 생성된 대여카드 번호.
     * @param member 대여카드 소유 회원.
     * @param rentStatus 초기 대여 가능 상태.
     * @param lateFee 초기 연체료.
     */
    private RentalCard(String rentalCardNo, RentalMember member, RentStatus rentStatus, LateFee lateFee) {
        this(rentalCardNo, member, rentStatus, lateFee, List.of(), List.of());
    }

    /**
     * 신규 생성/저장소 복원에서 내부 mutable 목록으로 상태를 초기화합니다.
     *
     * @param rentalCardNo 대여카드 번호.
     * @param member 대여카드 소유 회원.
     * @param rentStatus 현재 대여 가능 상태.
     * @param lateFee 현재 연체료.
     * @param rentItemList 현재 대여 중인 도서 목록.
     * @param returnItemList 반납 완료된 도서 목록.
     */
    private RentalCard(
        String rentalCardNo,
        RentalMember member,
        RentStatus rentStatus,
        LateFee lateFee,
        List<RentItem> rentItemList,
        List<ReturnItem> returnItemList
    ) {
        this.rentalCardNo = rentalCardNo;
        this.member = member;
        this.rentStatus = rentStatus;
        this.lateFee = lateFee;
        this.rentItemList = new ArrayList<>(rentItemList);
        this.returnItemList = new ArrayList<>(returnItemList);
    }

    /**
     * 회원에게 새 대여카드를 발급합니다.
     *
     * <p>번호 생성, 초기 상태, 빈 대여/반납 목록을 factory에서만 결정합니다.
     *
     * @param creator 대여카드를 발급받을 회원.
     * @return 신규 발급된 대여카드 aggregate.
     */
    public static RentalCard createRentalCard(RentalMember creator) {
        return new RentalCard(
            RentalCardNo.createRentalCardNo().no(),
            creator,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0)
        );
    }

    /**
     * 저장소 상태를 복원합니다.
     *
     * <p>복원은 새 업무 사건이 아니므로 domain event를 등록하지 않습니다.
     *
     * @param rentalCardNo 저장된 대여카드 번호.
     * @param member 저장된 대여카드 소유 회원.
     * @param rentStatus 저장된 대여 가능 상태.
     * @param lateFee 저장된 연체료.
     * @param rentItems 저장된 대여 중 도서 목록.
     * @param returnItems 저장된 반납 완료 도서 목록.
     * @return 저장 상태로 복원된 대여카드 aggregate.
     */
    public static RentalCard reconstitute(
        String rentalCardNo,
        RentalMember member,
        RentStatus rentStatus,
        LateFee lateFee,
        List<RentItem> rentItems,
        List<ReturnItem> returnItems
    ) {
        return new RentalCard(rentalCardNo, member, rentStatus, lateFee, rentItems, returnItems);
    }

    /**
     * 도서를 대여 목록에 추가하고 대여 완료 이벤트를 기록합니다.
     *
     * <p>대여 가능 여부, 한도, 중복을 먼저 검증하고 내부에 저장된 snapshot으로 이벤트를 만듭니다.
     *
     * @param item 대여할 도서 snapshot.
     */
    public void rentItem(RentalItem item) {
        if (rentStatus == RentStatus.RENT_UNAVAILABLE) {
            throw new IllegalArgumentException("대여 정지 상태에서는 도서를 대여할 수 없습니다.");
        }
        if (!RentalLimitPolicy.STANDARD.canRent(rentItemList.size())) {
            throw new IllegalArgumentException(
                "대여 중인 도서는 최대 " + RentalLimitPolicy.STANDARD.maxRentalCount() + "권까지 가능합니다."
            );
        }
        if (findRentItem(item) != null) {
            throw new IllegalArgumentException("이미 대여 중인 도서입니다.");
        }
        RentItem rentItem = RentItem.createRentalItem(item);
        rentItemList.add(rentItem);

        registerDomainEvent(
                ItemRentedDomainEvent.of(member, rentItem.item(), RentalPointPolicy.RENT.point())
        );
    }

    /**
     * 대여 중인 도서를 반납 목록으로 이동하고 반납 완료 이벤트를 기록합니다.
     *
     * <p>입력값이 아니라 내부 대여 항목 snapshot을 기준으로 이벤트와 연체료를 계산합니다.
     *
     * @param item 반납할 도서를 식별하는 snapshot.
     * @param returnDate 실제 반납일.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate.
     */
    public RentalCard returnItem(RentalItem item, LocalDate returnDate) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);

        long latePoint = calculateLatePoint(rentItem, returnDate);
        if (latePoint > 0) {
            lateFee = lateFee.addPoint(latePoint);
            rentStatus = RentStatus.RENT_UNAVAILABLE;
        }
        returnItemList.add(ReturnItem.createReturnItem(rentItem, returnDate));

        registerDomainEvent(
                ItemReturnedDomainEvent.of(member, rentItem.item(), RentalPointPolicy.RETURN.point())
        );
        return this;
    }

    /**
     * 대여 중인 도서를 연체 상태로 표시하고 대여카드를 대여 정지 상태로 변경합니다.
     *
     * <p>연체 표시는 내부 상태 변경만 필요하므로 domain event를 등록하지 않습니다.
     *
     * @param item 연체 처리할 도서를 식별하는 snapshot.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate.
     */
    public RentalCard overdueItem(RentalItem item) {
        RentItem rentItem = requireRentItem(item);
        int index = rentItemList.indexOf(rentItem);
        rentItemList.set(index, rentItem.markOverdue());
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return this;
    }

    /**
     * 누적 연체료를 포인트로 정산하고 대여 가능 상태로 전환합니다.
     *
     * <p>모든 도서가 반납됐고 입력 포인트가 현재 연체료와 일치할 때만 해제 이벤트를 기록합니다.
     *
     * @param point 정산할 연체료 포인트.
     * @return 실제 정산된 포인트.
     */
    public long makeAvailableRental(long point) {
        if (!rentItemList.isEmpty()) {
            throw new IllegalArgumentException("모든 도서를 반납해야 정지해제할 수 있습니다.");
        }
        if (lateFee.point() != point) {
            throw new IllegalArgumentException("입력 포인트가 현재 연체료와 일치하지 않습니다.");
        }
        lateFee = lateFee.removePoint(point);
        if (lateFee.point() == 0) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }

        registerDomainEvent(
                OverdueClearedDomainEvent.of(member, point)
        );
        return point;
    }

    /**
     * 대여 실패 보상 흐름에서 이미 반영된 도서 대여를 취소합니다.
     *
     * <p>대상이 없으면 멱등 처리하고, 있으면 내부 대여 항목 snapshot으로 취소 이벤트를 기록합니다.
     *
     * @param item 대여 취소할 도서를 식별하는 snapshot.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate.
     */
    public RentalCard cancelRentItem(RentalItem item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            return this;
        }
        rentItemList.remove(rentItem);
        if (lateFee.point() == 0 && rentItemList.stream().noneMatch(RentItem::overdue)) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        registerDomainEvent(
            ItemRentCanceledDomainEvent.of(member, rentItem.item(), RentalPointPolicy.RENT.point())
        );
        return this;
    }

    /**
     * 반납 실패 보상 흐름에서 이미 반영된 도서 반납을 취소합니다.
     *
     * <p>대상이 있으면 반납 목록의 snapshot을 대여 목록으로 되돌리고 그 snapshot으로 취소 이벤트를 기록합니다.
     *
     * @param item 반납 취소할 도서를 식별하는 snapshot.
     * @param point 회수해야 하는 반납 포인트.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate.
     */
    public RentalCard cancelReturnItem(RentalItem item, long point) {
        ReturnItem returnItem = findReturnItem(item);
        if (returnItem == null) {
            return this;
        }
        returnItemList.remove(returnItem);
        rentItemList.add(returnItem.item());

        long latePoint = calculateLatePoint(returnItem.item(), returnItem.returnDate());
        if (latePoint > 0) {
            lateFee = lateFee.removePoint(latePoint);
        }
        rentStatus = returnItem.item().overdue() || lateFee.point() > 0
            ? RentStatus.RENT_UNAVAILABLE
            : RentStatus.RENT_AVAILABLE;
        registerDomainEvent(
            ItemReturnCanceledDomainEvent.of(member, returnItem.item().item(), point)
        );
        return this;
    }

    /**
     * 연체 해제 실패 보상 흐름에서 정산된 연체 해제를 취소합니다.
     *
     * <p>이미 보상된 상태면 멱등 처리하고, 아니면 연체료와 대여 정지 상태를 복원합니다.
     *
     * @param point 다시 누적할 연체료 포인트.
     * @return 보상 처리에 사용된 포인트.
     */
    public long cancelMakeAvailableRental(long point) {
        if (rentStatus == RentStatus.RENT_UNAVAILABLE && lateFee.point() >= point) {
            return point;
        }
        lateFee = lateFee.addPoint(point);
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        registerDomainEvent(
            OverdueClearCanceledDomainEvent.of(member, point)
        );
        return point;
    }

    /**
     * 대여 목록에서 대상 도서를 찾고, 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot.
     * @return aggregate 내부에 저장된 대여 항목.
     */
    private RentItem requireRentItem(RentalItem item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            throw new IllegalArgumentException("대여 중인 도서가 아닙니다.");
        }
        return rentItem;
    }

    /**
     * Kafka metadata 없이 aggregate 내부 상태 변경 사실만 기록합니다.
     *
     * @param event aggregate 내부 상태 변경으로 발생한 도메인 이벤트.
     */
    private void registerDomainEvent(RentalDomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * 상태 변경과 이벤트 발행의 기준이 되는 내부 대여 항목을 찾습니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot.
     * @return 대여 중인 항목을 찾으면 내부 {@link RentItem}, 없으면 {@code null}.
     */
    private RentItem findRentItem(RentalItem item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    /**
     * 반납 목록에서 대상 도서를 찾고, 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot.
     * @return aggregate 내부에 저장된 반납 항목.
     */
    private ReturnItem requireReturnItem(RentalItem item) {
        ReturnItem returnItem = findReturnItem(item);
        if (returnItem == null) {
            throw new IllegalArgumentException("반납 완료된 도서가 아닙니다.");
        }
        return returnItem;
    }

    /**
     * 상태 변경과 이벤트 발행의 기준이 되는 내부 반납 항목을 찾습니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot.
     * @return 반납 항목을 찾으면 내부 {@link ReturnItem}, 없으면 {@code null}.
     */
    private ReturnItem findReturnItem(RentalItem item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.item().isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    /**
     * 대여 항목의 연체 기준일과 실제 반납일로 연체 포인트를 계산합니다.
     *
     * @param rentItem 연체료 계산 대상 대여 항목.
     * @param returnDate 실제 반납일.
     * @return 도메인 정책에 따라 계산된 연체 포인트.
     */
    private long calculateLatePoint(RentItem rentItem, LocalDate returnDate) {
        return RentalLateFeePolicy.DAILY.calculate(rentItem.overdueDate(), returnDate);
    }

    /**
     * @return 대여카드 번호입니다.
     */
    public String rentalCardNo() {
        return rentalCardNo;
    }

    /**
     * @return 대여카드 소유 회원 snapshot 입니다.
     */
    public RentalMember member() {
        return member;
    }

    /**
     * @return 현재 대여 가능 상태입니다.
     */
    public RentStatus rentStatus() {
        return rentStatus;
    }

    /**
     * @return 현재 누적 연체료입니다.
     */
    public LateFee lateFee() {
        return lateFee;
    }

    /**
     * 현재 대여 중인 항목 목록을 방어적 복사본으로 반환합니다.
     *
     * @return 외부에서 수정할 수 없는 대여 항목 목록입니다.
     */
    public List<RentItem> getRentItemList() {
        return List.copyOf(rentItemList);
    }

    /**
     * 기록된 domain event를 꺼내고, 중복 발행을 막기 위해 내부 버퍼를 비웁니다.
     *
     * @return 이번 상태 변경에서 발생한 domain event 목록.
     */
    public List<RentalDomainEvent> pullDomainEvents() {
        List<RentalDomainEvent> events = List.copyOf(domainEvents);
        // 발행 후 같은 도메인 이벤트가 다시 나가지 않도록 비웁니다.
        domainEvents.clear();
        return events;
    }

    /**
     * 현재 반납 완료된 항목 목록을 방어적 복사본으로 반환합니다.
     *
     * @return 외부에서 수정할 수 없는 반납 항목 목록.
     */
    public List<ReturnItem> getReturnItemList() {
        return List.copyOf(returnItemList);
    }
}
