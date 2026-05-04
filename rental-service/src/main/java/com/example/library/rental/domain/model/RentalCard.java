package com.example.library.rental.domain.model;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.vo.RentalCardNo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 회원의 대여 가능 상태, 대여/반납 목록, 연체료 규칙을 관리하는 대여카드 aggregate입니다.
 */
public class RentalCard {
    private static final int MAX_RENTAL_COUNT = 5;

    private final String rentalCardNo;
    private final IDName member;
    private RentStatus rentStatus;
    private LateFee lateFee;
    private final List<RentItem> rentItemList;
    private final List<ReturnItem> returnItemList;

    public RentalCard(String rentalCardNo, IDName member, RentStatus rentStatus, LateFee lateFee) {
        this(rentalCardNo, member, rentStatus, lateFee, List.of(), List.of());
    }

    private RentalCard(
        String rentalCardNo,
        IDName member,
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
     * 대여카드를 생성.
     * @param creator 대여카드를 생성할 회원의 식별 값.
     * @return 대여카드 반환.
     */
    public static RentalCard createRentalCard(IDName creator) {
        return new RentalCard(
            RentalCardNo.createRentalCardNo().no(),
            creator,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0)
        );
    }

    public static RentalCard reconstitute(
        String rentalCardNo,
        IDName member,
        RentStatus rentStatus,
        LateFee lateFee,
        List<RentItem> rentItems,
        List<ReturnItem> returnItems
    ) {
        return new RentalCard(rentalCardNo, member, rentStatus, lateFee, rentItems, returnItems);
    }

    /**
     * 도서 대여
     * @param item 업무 대상 도서의 번호와 제목
     */
    public void rentItem(Item item) {
        if (rentStatus == RentStatus.RENT_UNAVAILABLE) {
            throw new IllegalArgumentException("대여 정지 상태에서는 도서를 대여할 수 없습니다.");
        }
        if (rentItemList.size() >= MAX_RENTAL_COUNT) {
            throw new IllegalArgumentException("대여 중인 도서는 최대 5권까지 가능합니다.");
        }
        if (findRentItem(item) != null) {
            throw new IllegalArgumentException("이미 대여 중인 도서입니다.");
        }
        rentItemList.add(RentItem.createRentalItem(item));
    }

    public RentalCard returnItem(Item item, LocalDate returnDate) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);

        long latePoint = calculateLatePoint(rentItem, returnDate);
        if (latePoint > 0) {
            lateFee = lateFee.addPoint(latePoint);
            rentStatus = RentStatus.RENT_UNAVAILABLE;
        }
        returnItemList.add(ReturnItem.createReturnItem(rentItem, returnDate));
        return this;
    }

    public RentalCard overdueItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        int index = rentItemList.indexOf(rentItem);
        rentItemList.set(index, rentItem.markOverdued());
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return this;
    }

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
        return point;
    }

    public RentalCard cancelRentItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);
        if (lateFee.point() == 0 && rentItemList.stream().noneMatch(RentItem::overdued)) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return this;
    }

    public RentalCard cancelReturnItem(Item item, long point) {
        ReturnItem returnItem = requireReturnItem(item);
        returnItemList.remove(returnItem);
        rentItemList.add(returnItem.item());

        long latePoint = calculateLatePoint(returnItem.item(), returnItem.returnDate());
        if (latePoint > 0) {
            lateFee = lateFee.removePoint(latePoint);
        }
        rentStatus = returnItem.item().overdued() || lateFee.point() > 0
            ? RentStatus.RENT_UNAVAILABLE
            : RentStatus.RENT_AVAILABLE;
        return this;
    }

    public long cancelMakeAvailableRental(long point) {
        lateFee = lateFee.addPoint(point);
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return point;
    }

    public static ItemRented createItemRentedEvent(IDName idName, Item item, long point) {
        return createItemRentedEvent(UUID.randomUUID().toString(), idName, item, point);
    }

    public static ItemRented createItemRentedEvent(String correlationId, IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemRented(eventId, normalizeCorrelationId(correlationId, eventId), Instant.now(), idName, item, point);
    }

    public static ItemReturned createItemReturnEvent(IDName idName, Item item, long point) {
        return createItemReturnEvent(UUID.randomUUID().toString(), idName, item, point);
    }

    public static ItemReturned createItemReturnEvent(String correlationId, IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemReturned(eventId, normalizeCorrelationId(correlationId, eventId), Instant.now(), idName, item, point);
    }

    public static OverdueCleared createOverdueClearedEvent(IDName idName, long point) {
        return createOverdueClearedEvent(UUID.randomUUID().toString(), idName, point);
    }

    public static OverdueCleared createOverdueClearedEvent(String correlationId, IDName idName, long point) {
        String eventId = UUID.randomUUID().toString();
        return new OverdueCleared(eventId, normalizeCorrelationId(correlationId, eventId), Instant.now(), idName, point);
    }

    private static String normalizeCorrelationId(String correlationId, String eventId) {
        return correlationId == null || correlationId.isBlank() ? eventId : correlationId;
    }

    private RentItem requireRentItem(Item item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            throw new IllegalArgumentException("대여 중인 도서가 아닙니다.");
        }
        return rentItem;
    }

    /**
     * 대여 도서 확인.
     * @param item 업무 대상 도서의 번호와 제목.
     * @return 대여 중인 단일 도서와 대여 상태 반환.
     */
    private RentItem findRentItem(Item item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    private ReturnItem requireReturnItem(Item item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.item().isSameItem(item))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("반납 완료된 도서가 아닙니다."));
    }

    private long calculateLatePoint(RentItem rentItem, LocalDate returnDate) {
        if (returnDate.isAfter(rentItem.overdueDate())) {
            return ChronoUnit.DAYS.between(rentItem.overdueDate(), returnDate) * 10;
        }
        return 0;
    }

    public String getRentalCardNo() {
        return rentalCardNo;
    }

    public IDName getMember() {
        return member;
    }

    public RentStatus getRentStatus() {
        return rentStatus;
    }

    public LateFee getLateFee() {
        return lateFee;
    }

    public List<RentItem> getRentItemList() {
        return Collections.unmodifiableList(rentItemList);
    }

    public List<ReturnItem> getReturnItemList() {
        return Collections.unmodifiableList(returnItemList);
    }
}
