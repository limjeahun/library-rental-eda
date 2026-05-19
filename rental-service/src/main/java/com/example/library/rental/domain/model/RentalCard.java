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
 * 회원의 대여 가능 상태, 대여/반납 목록, 연체료를 관리하는 대여 aggregate root 입니다.
 *
 * <p>대여카드의 상태 변경은 이 aggregate 의 행위 메서드를 통해서만 수행합니다. 도서 대여, 반납,
 * 연체 표시, 연체 해제, 보상 취소 같은 업무 규칙은 내부 상태를 먼저 변경한 뒤 service-local domain event 를
 * 기록합니다. application service 는 aggregate 를 저장한 뒤 {@link #pullDomainEvents()} 로 이번 상태 변경에서
 * 발생한 이벤트를 꺼내 outbound port 로 전달합니다.
 *
 * <p>aggregate root 는 Lombok 으로 accessor 를 생성하지 않고, 필요한 조회 메서드를 명시적으로 작성합니다.
 * 예를 들어 회원 snapshot 은 {@link #member()} 처럼 record canonical accessor 와 같은 형태의 메서드로 노출합니다.
 *
 * <p>이 클래스는 순수 도메인 모델이므로 Spring, JPA, Kafka, common-events 메시지 계약을 알지 않습니다.
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
     * 신규 대여카드 생성을 위한 내부 생성자입니다.
     *
     * <p>외부 코드는 {@link #createRentalCard(RentalMember)} 를 통해 새 aggregate 를 만들도록 제한합니다.
     * 생성 경로를 factory 로 모아야 기본 대여 가능 상태, 빈 대여/반납 목록, 연체료 0원 같은 생성 규칙을
     * 한 곳에서 유지할 수 있습니다.
     *
     * @param rentalCardNo 생성된 대여카드 번호입니다.
     * @param member 대여카드 소유 회원입니다.
     * @param rentStatus 초기 대여 가능 상태입니다.
     * @param lateFee 초기 연체료입니다.
     */
    private RentalCard(String rentalCardNo, RentalMember member, RentStatus rentStatus, LateFee lateFee) {
        this(rentalCardNo, member, rentStatus, lateFee, List.of(), List.of());
    }

    /**
     * 신규 생성과 저장소 복원 모두에서 사용하는 전체 상태 초기화 생성자입니다.
     *
     * <p>전달받은 대여/반납 목록은 내부 mutable list 로 복사합니다. 외부 컬렉션 참조를 그대로 보관하지 않아야
     * aggregate 외부에서 내부 상태를 우회 변경할 수 없습니다. domain event 버퍼는 필드 초기값으로 항상 비어
     * 있으므로 저장소에서 복원된 aggregate 가 과거 이벤트를 다시 발행하지 않습니다.
     *
     * @param rentalCardNo 대여카드 번호입니다.
     * @param member 대여카드 소유 회원입니다.
     * @param rentStatus 현재 대여 가능 상태입니다.
     * @param lateFee 현재 연체료입니다.
     * @param rentItemList 현재 대여 중인 도서 목록입니다.
     * @param returnItemList 반납 완료된 도서 목록입니다.
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
     * <p>신규 aggregate 생성 규칙을 표현하는 factory 입니다. 대여카드 번호는 도메인 값 객체가 생성하고,
     * 대여 상태는 가능 상태, 연체료는 0, 대여/반납 목록은 빈 목록으로 시작합니다.
     *
     * @param creator 대여카드를 발급받을 회원입니다.
     * @return 신규 발급된 대여카드 aggregate 입니다.
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
     * 저장소에 보관된 상태로 대여카드 aggregate 를 복원합니다.
     *
     * <p>복원은 신규 업무 이벤트가 아니므로 domain event 를 등록하지 않습니다. persistence adapter 는 JPA 엔티티를
     * 도메인 모델로 되돌릴 때 이 factory 를 사용하고, 복원된 aggregate 는 이후 새 상태 변경에서 발생한 이벤트만
     * {@link #pullDomainEvents()} 로 제공해야 합니다.
     *
     * @param rentalCardNo 저장된 대여카드 번호입니다.
     * @param member 저장된 대여카드 소유 회원입니다.
     * @param rentStatus 저장된 대여 가능 상태입니다.
     * @param lateFee 저장된 연체료입니다.
     * @param rentItems 저장된 대여 중 도서 목록입니다.
     * @param returnItems 저장된 반납 완료 도서 목록입니다.
     * @return 저장 상태로 복원된 대여카드 aggregate 입니다.
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
     * 도서를 대여 목록에 추가하고 대여 완료 도메인 이벤트를 기록합니다.
     *
     * <p>대여 정지 상태, 대여 한도 초과, 중복 대여를 aggregate 내부에서 검증합니다. 검증을 통과하면 입력 도서를
     * aggregate 의 대여 항목으로 만들고, 실제 내부에 반영된 {@link RentItem#item()} snapshot 으로
     * {@link ItemRentedDomainEvent} 를 등록합니다.
     *
     * @param item 대여할 도서 snapshot 입니다.
     * @throws IllegalArgumentException 대여 정지 상태이거나, 대여 한도를 초과했거나, 이미 대여 중인 도서인 경우입니다.
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
     * 대여 중인 도서를 반납 목록으로 이동하고 반납 완료 도메인 이벤트를 기록합니다.
     *
     * <p>반납 대상은 입력값이 아니라 aggregate 내부 대여 목록에서 찾은 {@link RentItem} 입니다. 따라서 이벤트도
     * 호출자가 넘긴 값이 아닌 실제 대여 중이던 도서 snapshot 을 사용합니다. 반납일 기준으로 연체료가 발생하면
     * 연체료를 누적하고 대여 상태를 정지 상태로 전환합니다.
     *
     * @param item 반납할 도서를 식별하는 snapshot 입니다.
     * @param returnDate 실제 반납일입니다.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate 입니다.
     * @throws IllegalArgumentException 대여 중인 도서가 아닌 경우입니다.
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
     * <p>이 메서드는 대여카드 내부 상태만 변경합니다. 현재 흐름에서는 연체 표시 자체를 외부 Kafka 이벤트로 발행하지
     * 않으므로 domain event 를 등록하지 않습니다.
     *
     * @param item 연체 처리할 도서를 식별하는 snapshot 입니다.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate 입니다.
     * @throws IllegalArgumentException 대여 중인 도서가 아닌 경우입니다.
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
     * <p>모든 도서가 반납된 상태에서만 연체 해제가 가능하며, 입력 포인트는 현재 연체료와 정확히 일치해야 합니다.
     * 정산 후 연체료가 0이 되면 대여 가능 상태로 변경하고 {@link OverdueClearedDomainEvent} 를 등록합니다.
     *
     * @param point 정산할 연체료 포인트입니다.
     * @return 실제 정산된 포인트입니다.
     * @throws IllegalArgumentException 아직 대여 중인 도서가 있거나 입력 포인트가 현재 연체료와 다를 경우입니다.
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
     * <p>취소 대상이 대여 목록에 없으면 이미 보상되었거나 처리할 상태가 없는 것으로 보고 아무 이벤트도 등록하지
     * 않습니다. 취소 대상이 있으면 aggregate 내부 대여 항목을 제거하고, 내부에 저장되어 있던 도서 snapshot 으로
     * {@link ItemRentCanceledDomainEvent} 를 등록합니다.
     *
     * @param item 대여 취소할 도서를 식별하는 snapshot 입니다.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate 입니다.
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
     * <p>취소 대상이 반납 목록에 없으면 이미 보상되었거나 처리할 상태가 없는 것으로 보고 아무 이벤트도 등록하지
     * 않습니다. 대상이 있으면 반납 목록에서 제거한 뒤 대여 목록으로 되돌리고, 반납 당시 발생했던 연체료를 다시
     * 차감합니다. 이벤트에는 호출 입력값이 아니라 반납 목록에 저장되어 있던 도서 snapshot 을 사용합니다.
     *
     * @param item 반납 취소할 도서를 식별하는 snapshot 입니다.
     * @param point 회수해야 하는 반납 포인트입니다.
     * @return 상태 변경이 반영된 현재 대여카드 aggregate 입니다.
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
     * <p>이미 대여 정지 상태이고 연체료가 보상 포인트 이상이면 같은 보상 command 가 재전달된 것으로 보고 상태를
     * 변경하지 않습니다. 그렇지 않으면 연체료를 다시 누적하고 대여 정지 상태로 전환한 뒤
     * {@link OverdueClearCanceledDomainEvent} 를 등록합니다.
     *
     * @param point 다시 누적할 연체료 포인트입니다.
     * @return 보상 처리에 사용된 포인트입니다.
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
     * 대여 목록에서 대상 도서를 찾아 반환하고, 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot 입니다.
     * @return aggregate 내부에 저장된 대여 항목입니다.
     * @throws IllegalArgumentException 대여 중인 도서가 아닌 경우입니다.
     */
    private RentItem requireRentItem(RentalItem item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            throw new IllegalArgumentException("대여 중인 도서가 아닙니다.");
        }
        return rentItem;
    }

    /**
     * 이번 aggregate 상태 변경에서 발생한 service-local domain event 를 임시 버퍼에 기록합니다.
     *
     * <p>이벤트는 Kafka 메시지가 아니라 도메인 상태 변경 사실입니다. eventId, correlationId, topic 같은 통합 메시지
     * 메타데이터는 adapter 에서 부여합니다.
     *
     * @param event aggregate 내부 상태 변경으로 발생한 도메인 이벤트입니다.
     */
    private void registerDomainEvent(RentalDomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * 대여 목록에서 도서 번호가 같은 대여 항목을 찾습니다.
     *
     * <p>반환값은 호출 입력값이 아니라 aggregate 내부에 저장된 {@link RentItem} 입니다. 상태 변경과 이벤트 발행은
     * 이 내부 객체를 기준으로 수행해야 외부 입력값이 aggregate 의 실제 snapshot 을 덮어쓰지 않습니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot 입니다.
     * @return 대여 중인 항목을 찾으면 내부 {@link RentItem}, 없으면 {@code null} 입니다.
     */
    private RentItem findRentItem(RentalItem item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    /**
     * 반납 목록에서 대상 도서를 찾아 반환하고, 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot 입니다.
     * @return aggregate 내부에 저장된 반납 항목입니다.
     * @throws IllegalArgumentException 반납 완료된 도서가 아닌 경우입니다.
     */
    private ReturnItem requireReturnItem(RentalItem item) {
        ReturnItem returnItem = findReturnItem(item);
        if (returnItem == null) {
            throw new IllegalArgumentException("반납 완료된 도서가 아닙니다.");
        }
        return returnItem;
    }

    /**
     * 반납 목록에서 도서 번호가 같은 반납 항목을 찾습니다.
     *
     * @param item 찾을 도서를 식별하는 snapshot 입니다.
     * @return 반납 항목을 찾으면 내부 {@link ReturnItem}, 없으면 {@code null} 입니다.
     */
    private ReturnItem findReturnItem(RentalItem item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.item().isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    /**
     * 대여 항목의 연체 기준일과 실제 반납일을 기준으로 연체 포인트를 계산합니다.
     *
     * @param rentItem 연체료 계산 대상 대여 항목입니다.
     * @param returnDate 실제 반납일입니다.
     * @return 도메인 정책에 따라 계산된 연체 포인트입니다.
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
     * 이번 상태 변경 과정에서 기록된 domain event 를 꺼내고 내부 버퍼를 비웁니다.
     *
     * <p>application service 는 aggregate 를 저장한 뒤 이 메서드를 한 번 호출해 필요한 이벤트를 outbound port 로
     * 전달합니다. 반환 이후 내부 이벤트 버퍼는 비워지므로 같은 상태 변경 이벤트가 중복 발행되지 않습니다.
     *
     * @return 이번 상태 변경에서 발생한 domain event 목록입니다.
     */
    public List<RentalDomainEvent> pullDomainEvents() {
        List<RentalDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * 현재 반납 완료된 항목 목록을 외부에서 수정할 수 없는 형태로 반환합니다.
     *
     * @return 외부에서 수정할 수 없는 반납 항목 목록입니다.
     */
    public List<ReturnItem> getReturnItemList() {
        return List.copyOf(returnItemList);
    }
}
