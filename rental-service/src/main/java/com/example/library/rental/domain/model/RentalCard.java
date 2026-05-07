package com.example.library.rental.domain.model;

import com.example.library.rental.domain.vo.RentalMember;
import com.example.library.rental.domain.vo.RentalItem;
import com.example.library.rental.domain.vo.LateFee;
import com.example.library.rental.domain.vo.RentalCardNo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 회원의 대여 가능 상태, 대여/반납 목록, 연체료 규칙을 관리하는 대여카드.
 */
public class RentalCard {
    private final String rentalCardNo;
    private final RentalMember member;
    private RentStatus rentStatus;
    private LateFee lateFee;
    private final List<RentItem> rentItemList;
    private final List<ReturnItem> returnItemList;

    public RentalCard(String rentalCardNo, RentalMember member, RentStatus rentStatus, LateFee lateFee) {
        this(rentalCardNo, member, rentStatus, lateFee, List.of(), List.of());
    }

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
     * 대여카드를 생성.
     * @param creator 대여카드를 생성할 회원의 식별 값.
     * @return 대여카드 반환.
     */
    public static RentalCard createRentalCard(RentalMember creator) {
        return new RentalCard(
            RentalCardNo.createRentalCardNo().no(),
            creator,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0)
        );
    }

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
     * 도서 대여
     * @param item 업무 대상 도서의 번호와 제목
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
        rentItemList.add(RentItem.createRentalItem(item));
    }

    public RentalCard returnItem(RentalItem item, LocalDate returnDate) {
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

    public RentalCard overdueItem(RentalItem item) {
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

    public RentalCard cancelRentItem(RentalItem item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            return this;
        }
        rentItemList.remove(rentItem);
        if (lateFee.point() == 0 && rentItemList.stream().noneMatch(RentItem::overdued)) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return this;
    }

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
        rentStatus = returnItem.item().overdued() || lateFee.point() > 0
            ? RentStatus.RENT_UNAVAILABLE
            : RentStatus.RENT_AVAILABLE;
        return this;
    }

    public long cancelMakeAvailableRental(long point) {
        if (rentStatus == RentStatus.RENT_UNAVAILABLE && lateFee.point() >= point) {
            return point;
        }
        lateFee = lateFee.addPoint(point);
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return point;
    }

    private RentItem requireRentItem(RentalItem item) {
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
    private RentItem findRentItem(RentalItem item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    private ReturnItem requireReturnItem(RentalItem item) {
        ReturnItem returnItem = findReturnItem(item);
        if (returnItem == null) {
            throw new IllegalArgumentException("반납 완료된 도서가 아닙니다.");
        }
        return returnItem;
    }

    private ReturnItem findReturnItem(RentalItem item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.item().isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    private long calculateLatePoint(RentItem rentItem, LocalDate returnDate) {
        return RentalLateFeePolicy.DAILY.calculate(rentItem.overdueDate(), returnDate);
    }

    public String getRentalCardNo() {
        return rentalCardNo;
    }

    public RentalMember getMember() {
        return member;
    }

    public RentStatus getRentStatus() {
        return rentStatus;
    }

    public LateFee getLateFee() {
        return lateFee;
    }

    public List<RentItem> getRentItemList() {
        return List.copyOf(rentItemList);
    }

    public List<ReturnItem> getReturnItemList() {
        return Collections.unmodifiableList(returnItemList);
    }
}
