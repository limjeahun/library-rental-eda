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

/**
 * 회원의 대여 가능 상태, 대여/반납 목록, 연체료 규칙을 관리하는 대여카드 도메인 모델입니다.
 */
public class RentalCard {
    private static final int MAX_RENTAL_COUNT = 5;

    private String rentalCardNo;
    private IDName member;
    private RentStatus rentStatus;
    private LateFee lateFee;
    private List<RentItem> rentItemList = new ArrayList<>();
    private List<ReturnItem> returnItemList = new ArrayList<>();

    /**
     * 프레임워크 바인딩과 영속성 복원을 위한 기본 생성자입니다.
     */
    public RentalCard() {
    }

    /**
     * 저장된 대여카드 식별자, 회원, 상태, 연체료를 가진 도메인 모델을 생성합니다.
     *
     * @param rentalCardNo 설정할 대여카드 번호입니다.
     * @param member 대여카드 소유 회원의 ID와 이름입니다.
     * @param rentStatus 저장하거나 설정할 대여카드 상태입니다.
     * @param lateFee 저장하거나 설정할 연체료 값 객체입니다.
     */
    public RentalCard(String rentalCardNo, IDName member, RentStatus rentStatus, LateFee lateFee) {
        this.rentalCardNo = rentalCardNo;
        this.member = member;
        this.rentStatus = rentStatus;
        this.lateFee = lateFee;
    }

    /**
     * 새 회원의 대여카드를 대여 가능 상태와 0 연체료로 생성합니다.
     *
     * @param creator 대여카드를 생성할 회원의 식별 값입니다.
     * @return 대여 가능 상태와 0 연체료로 초기화된 새 대여카드를 반환합니다.
     */
    public static RentalCard createRentalCard(IDName creator) {
        return new RentalCard(
            RentalCardNo.createRentalCardNo().getNo(),
            creator,
            RentStatus.RENT_AVAILABLE,
            new LateFee(0)
        );
    }

    /**
     * 대여 가능 여부, 대여 권수, 중복 대여 규칙을 검증하고 도서를 대여 목록에 추가합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대여 목록에 도서가 추가된 현재 대여카드를 반환합니다.
     */
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

    /**
     * 대여 중인 도서를 반납 처리하고 연체료가 발생하면 대여카드를 정지 상태로 변경합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     * @return 대여 목록에서 제거되고 반납 목록과 연체료가 갱신된 현재 대여카드를 반환합니다.
     */
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

    /**
     * 대여 중인 도서를 연체 상태로 표시하고 대여카드를 정지 상태로 변경합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대상 도서가 연체 표시되고 대여 정지 상태가 된 현재 대여카드를 반환합니다.
     */
    public RentalCard overdueItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        rentItem.markOverdued();
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return this;
    }

    /**
     * 모든 도서가 반납되고 입력 포인트가 연체료와 일치할 때 대여 정지를 해제합니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 연체료 정산에 사용된 포인트 값을 반환합니다.
     */
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

    /**
     * 대여 성공 후 참여 서비스 실패 시 대여 내역을 취소하는 보상 동작입니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대여 보상으로 대상 도서가 대여 목록에서 제거된 현재 대여카드를 반환합니다.
     */
    public RentalCard cancleRentItem(Item item) {
        RentItem rentItem = requireRentItem(item);
        rentItemList.remove(rentItem);
        if (lateFee.getPoint() == 0 && rentItemList.stream().noneMatch(RentItem::isOverdued)) {
            rentStatus = RentStatus.RENT_AVAILABLE;
        }
        return this;
    }

    /**
     * 대여 취소 보상 동작의 올바른 철자 alias입니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대여 보상으로 대상 도서가 대여 목록에서 제거된 현재 대여카드를 반환합니다.
     */
    public RentalCard cancelRentItem(Item item) {
        return cancleRentItem(item);
    }

    /**
     * 반납 성공 후 참여 서비스 실패 시 반납 내역을 되돌리는 보상 동작입니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 반납 보상으로 반납 목록이 되돌려지고 대여 목록이 복원된 현재 대여카드를 반환합니다.
     */
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

    /**
     * 반납 취소 보상 동작의 올바른 철자 alias입니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 반납 보상으로 반납 목록이 되돌려지고 대여 목록이 복원된 현재 대여카드를 반환합니다.
     */
    public RentalCard cancelReturnItem(Item item, long point) {
        return cancleReturnItem(item, point);
    }

    /**
     * 연체 해제 성공 후 참여 서비스 실패 시 연체료와 정지 상태를 복구하는 보상 동작입니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 연체 해제 보상으로 다시 부과한 포인트 값을 반환합니다.
     */
    public long cancleMakeAvailableRental(long point) {
        lateFee.addPoint(point);
        rentStatus = RentStatus.RENT_UNAVAILABLE;
        return point;
    }

    /**
     * 연체 해제 취소 보상 동작의 올바른 철자 alias입니다.
     *
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 연체 해제 보상으로 다시 부과한 포인트 값을 반환합니다.
     */
    public long cancelMakeAvailableRental(long point) {
        return cancleMakeAvailableRental(point);
    }

    /**
     * 도서 대여 완료 사실을 알리는 공통 도메인 이벤트를 생성합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 새 eventId/correlationId, 회원, 도서, 적립 포인트를 담은 ItemRented 이벤트를 반환합니다.
     */
    public static ItemRented createItemRentedEvent(IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemRented(eventId, eventId, Instant.now(), idName, item, point);
    }

    /**
     * 도서 반납 완료 사실을 알리는 공통 도메인 이벤트를 생성합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 새 eventId/correlationId, 회원, 도서, 적립 포인트를 담은 ItemReturned 이벤트를 반환합니다.
     */
    public static ItemReturned createItemReturnEvent(IDName idName, Item item, long point) {
        String eventId = UUID.randomUUID().toString();
        return new ItemReturned(eventId, eventId, Instant.now(), idName, item, point);
    }

    /**
     * 연체 해제 완료 사실을 알리는 공통 도메인 이벤트를 생성합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 새 eventId/correlationId, 회원, 정산 포인트를 담은 OverdueCleared 이벤트를 반환합니다.
     */
    public static OverdueCleared createOverdueCleardEvent(IDName idName, long point) {
        String eventId = UUID.randomUUID().toString();
        return new OverdueCleared(eventId, eventId, Instant.now(), idName, point);
    }

    /**
     * 연체 해제 이벤트 생성 동작의 올바른 철자 alias입니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
     * @return 새 eventId/correlationId, 회원, 정산 포인트를 담은 OverdueCleared 이벤트를 반환합니다.
     */
    public static OverdueCleared createOverdueClearedEvent(IDName idName, long point) {
        return createOverdueCleardEvent(idName, point);
    }

    /**
     * 대여 목록에서 대상 도서를 찾고 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대여 목록에서 찾은 대상 대여 항목을 반환합니다.
     */
    private RentItem requireRentItem(Item item) {
        RentItem rentItem = findRentItem(item);
        if (rentItem == null) {
            throw new IllegalArgumentException("대여 중인 도서가 아닙니다.");
        }
        return rentItem;
    }

    /**
     * 대여 목록에서 같은 도서 번호를 가진 대여 항목을 찾습니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 같은 도서 번호를 가진 대여 항목을 반환하거나 없으면 null을 반환합니다.
     */
    private RentItem findRentItem(Item item) {
        return rentItemList.stream()
            .filter(rentItem -> rentItem.isSameItem(item))
            .findFirst()
            .orElse(null);
    }

    /**
     * 반납 목록에서 대상 도서를 찾고 없으면 도메인 예외를 발생시킵니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 반납 목록에서 찾은 대상 반납 항목을 반환합니다.
     */
    private ReturnItem requireReturnItem(Item item) {
        return returnItemList.stream()
            .filter(returnItem -> returnItem.getItem().isSameItem(item))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("반납 완료된 도서가 아닙니다."));
    }

    /**
     * 반납일이 연체일 이후인 경우 일 단위 연체 포인트를 계산합니다.
     *
     * @param rentItem 반납 또는 연체료 계산 대상 대여 항목입니다.
     * @param returnDate 도서가 실제로 반납된 날짜입니다.
     * @return 반납 예정일 이후 경과일 수에 10포인트를 곱한 연체료를 반환합니다.
     */
    private long calculateLatePoint(RentItem rentItem, LocalDate returnDate) {
        if (returnDate.isAfter(rentItem.getOverdueDate())) {
            return ChronoUnit.DAYS.between(rentItem.getOverdueDate(), returnDate) * 10;
        }
        return 0;
    }

    /**
     * 대여카드 번호를 반환합니다.
     *
     * @return 대여카드 번호 문자열을 반환합니다.
     */
    public String getRentalCardNo() {
        return rentalCardNo;
    }

    /**
     * 대여카드 번호를 설정합니다.
     *
     * @param rentalCardNo 설정할 대여카드 번호입니다.
     */
    public void setRentalCardNo(String rentalCardNo) {
        this.rentalCardNo = rentalCardNo;
    }

    /**
     * 대여카드 소유 회원을 반환합니다.
     *
     * @return 대여카드 소유 회원의 ID와 이름을 반환합니다.
     */
    public IDName getMember() {
        return member;
    }

    /**
     * 대여카드 소유 회원을 설정합니다.
     *
     * @param member 대여카드 소유 회원의 ID와 이름입니다.
     */
    public void setMember(IDName member) {
        this.member = member;
    }

    /**
     * 현재 대여 가능 상태를 반환합니다.
     *
     * @return 대여 가능 또는 대여 정지 상태를 반환합니다.
     */
    public RentStatus getRentStatus() {
        return rentStatus;
    }

    /**
     * 현재 대여 가능 상태를 설정합니다.
     *
     * @param rentStatus 저장하거나 설정할 대여카드 상태입니다.
     */
    public void setRentStatus(RentStatus rentStatus) {
        this.rentStatus = rentStatus;
    }

    /**
     * 현재 연체료 값을 반환합니다.
     *
     * @return 현재 누적 연체료 포인트를 담은 값 객체를 반환합니다.
     */
    public LateFee getLateFee() {
        return lateFee;
    }

    /**
     * 현재 연체료 값을 설정합니다.
     *
     * @param lateFee 저장하거나 설정할 연체료 값 객체입니다.
     */
    public void setLateFee(LateFee lateFee) {
        this.lateFee = lateFee;
    }

    /**
     * 현재 대여 중인 도서 목록을 반환합니다.
     *
     * @return 현재 대여 중인 도서 목록을 반환합니다.
     */
    public List<RentItem> getRentItemList() {
        return rentItemList;
    }

    /**
     * 현재 대여 중인 도서 목록을 설정합니다.
     *
     * @param rentItemList 설정할 현재 대여 중 도서 목록입니다.
     */
    public void setRentItemList(List<RentItem> rentItemList) {
        this.rentItemList = rentItemList;
    }

    /**
     * 반납 완료된 도서 목록을 반환합니다.
     *
     * @return 반납 완료된 도서 목록을 반환합니다.
     */
    public List<ReturnItem> getReturnItemList() {
        return returnItemList;
    }

    /**
     * 반납 완료된 도서 목록을 설정합니다.
     *
     * @param returnItemList 설정할 반납 완료 도서 목록입니다.
     */
    public void setReturnItemList(List<ReturnItem> returnItemList) {
        this.returnItemList = returnItemList;
    }
}
