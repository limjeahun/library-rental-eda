package com.example.library.rental.domain.model;

import com.example.library.common.event.ItemRented;
import com.example.library.common.event.ItemReturned;
import com.example.library.common.event.OverdueCleared;
import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RentalCard {
    private static final int MAX_RENTAL_COUNT = 5;

    private String rentalCardNo;
    private IDName member;
    private RentStatus rentStatus;
    private LateFee lateFee;
    private List<RentItem> rentItemList = new ArrayList<>();
    private List<ReturnItem> returnItemList = new ArrayList<>();

    public RentalCard() {
    }

    public RentalCard(String rentalCardNo, IDName member, RentStatus rentStatus, LateFee lateFee) {
        this.rentalCardNo = rentalCardNo;
        this.member = member;
        this.rentStatus = rentStatus;
        this.lateFee = lateFee;
    }

    public static RentalCard createRentalCard(IDName creator) {
        return new RentalCard(
            RentalCardNo.createRentalCardNo().getNo(),
            creator,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0)
        );
    }

    public RentalCard rentItem(Item item) {
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
        return this;
    }

    public RentalCard returnItem(Item item, LocalDate returnDate) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);

        long latePoint = calculateLatePoint(rentItem, returnDate);
        if (latePoint > 0) {
            lateFee.addPoint(latePoint);
            rentStatus = RentStatus.RENT_UNAVAILABLE;
        }
        returnItemList.add(ReturnItem.createReturnItem(rentItem, returnDate));
        return this;
    }

    public RentalCard overdueItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        rentItem.markOverdued();
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return this;
    }

    public long makeAvailableRental(long point) {
        if (!rentItemList.isEmpty()) {
            throw new IllegalArgumentException("모든 도서를 반납해야 정지해제할 수 있습니다.");
        }
        if (lateFee.getPoint() != point) {
            throw new IllegalArgumentException("입력 포인트가 현재 연체료와 일치하지 않습니다.");
        }
        lateFee.removePoint(point);
        if (lateFee.getPoint() == 0) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return point;
    }

    public RentalCard cancleRentItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);
        if (lateFee.getPoint() == 0 && rentItemList.stream().noneMatch(RentItem::isOverdued)) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return this;
    }

    public RentalCard cancelRentItem(Item item) {
        return cancleRentItem(item);
    }

    public RentalCard cancleReturnItem(Item item, long point) {
        ReturnItem returnItem = requireReturnItem(item);
        returnItemList.remove(returnItem);
        rentItemList.add(returnItem.getItem());

        long latePoint = calculateLatePoint(returnItem.getItem(), returnItem.getReturnDate());
        if (latePoint > 0) {
            lateFee.removePoint(latePoint);
        }
        if (returnItem.getItem().isOverdued() || lateFee.getPoint() > 0) {
            rentStatus = RentStatus.RENT_UNAVAILABLE;
        } else {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return this;
    }

    public RentalCard cancelReturnItem(Item item, long point) {
        return cancleReturnItem(item, point);
    }

    public long cancleMakeAvailableRental(long point) {
        lateFee.addPoint(point);
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return point;
    }

    public long cancelMakeAvailableRental(long point) {
        return cancleMakeAvailableRental(point);
    }

    public static ItemRented createItemRentedEvent(IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemRented(eventId, eventId, Instant.now(), idName, item, point);
    }

    public static ItemReturned createItemReturnEvent(IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemReturned(eventId, eventId, Instant.now(), idName, item, point);
    }

    public static OverdueCleared createOverdueCleardEvent(IDName idName, long point) {
        String eventId = UUID.randomUUID().toString();
        return new OverdueCleared(eventId, eventId, Instant.now(), idName, point);
    }

    public static OverdueCleared createOverdueClearedEvent(IDName idName, long point) {
        return createOverdueCleardEvent(idName, point);
    }

    private RentItem requireRentItem(Item item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            throw new IllegalArgumentException("대여 중인 도서가 아닙니다.");
        }
        return rentItem;
    }

    private RentItem findRentItem(Item item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    private ReturnItem requireReturnItem(Item item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.getItem().isSameItem(item))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("반납 완료된 도서가 아닙니다."));
    }

    private long calculateLatePoint(RentItem rentItem, LocalDate returnDate) {
        if (returnDate.isAfter(rentItem.getOverdueDate())) {
            return ChronoUnit.DAYS.between(rentItem.getOverdueDate(), returnDate) * 10;
        }
        return 0;
    }

    public String getRentalCardNo() {
        return rentalCardNo;
    }

    public void setRentalCardNo(String rentalCardNo) {
        this.rentalCardNo = rentalCardNo;
    }

    public IDName getMember() {
        return member;
    }

    public void setMember(IDName member) {
        this.member = member;
    }

    public RentStatus getRentStatus() {
        return rentStatus;
    }

    public void setRentStatus(RentStatus rentStatus) {
        this.rentStatus = rentStatus;
    }

    public LateFee getLateFee() {
        return lateFee;
    }

    public void setLateFee(LateFee lateFee) {
        this.lateFee = lateFee;
    }

    public List<RentItem> getRentItemList() {
        return rentItemList;
    }

    public void setRentItemList(List<RentItem> rentItemList) {
        this.rentItemList = rentItemList;
    }

    public List<ReturnItem> getReturnItemList() {
        return returnItemList;
    }

    public void setReturnItemList(List<ReturnItem> returnItemList) {
        this.returnItemList = returnItemList;
    }
}
