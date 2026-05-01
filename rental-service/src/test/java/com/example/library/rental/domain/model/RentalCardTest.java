package com.example.library.rental.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 대여카드 도메인 모델의 대여, 반납, 연체, 보상 규칙을 검증합니다.
 */
class RentalCardTest {
    /**
     * 대여 가능 상태에서 도서를 대여 목록에 추가할 수 있는지 검증합니다. @Test void rentItem() { RentalCard rentalCard = RentalCard.createRentalCard(member()); rentalCard.rentItem(item(1)); assertThat(rentalCard.getRentItemList()).hasSize(1); assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE); } 대여 중인 도서가 최대 5권으로 제한되는지 검증합니다. @Test void rentItemLimitIsFive() { RentalCard rentalCard = RentalCard.createRentalCard(member()); for (long i = 1; i <= 5; i++) { rentalCard.rentItem(item(i)); } assertThatThrownBy(() -> rentalCard.rentItem(item(6))) .isInstanceOf(IllegalArgumentException.class); } 반납 시 대여 목록에서 제거되고 반납 목록에 추가되는지 검증합니다. @Test void returnItem() { RentalCard rentalCard = RentalCard.createRentalCard(member()); Item item = item(1); rentalCard.rentItem(item); rentalCard.returnItem(item, LocalDate.now()); assertThat(rentalCard.getRentItemList()).isEmpty(); assertThat(rentalCard.getReturnItemList()).hasSize(1); assertThat(rentalCard.getLateFee().getPoint()).isZero(); } 반납 예정일 이후 반납 시 연체료가 계산되고 대여가 정지되는지 검증합니다. @Test void calculateLateFee() { RentalCard rentalCard = RentalCard.createRentalCard(member()); Item item = item(1); rentalCard.rentItem(item); rentalCard.getRentItemList().getFirst().setOverdueDate(LocalDate.now().minusDays(3)); rentalCard.returnItem(item, LocalDate.now()); assertThat(rentalCard.getLateFee().getPoint()).isEqualTo(30); assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE); } 연체료를 정확히 정산하면 대여 정지가 해제되는지 검증합니다. @Test void clearOverdue() { RentalCard rentalCard = RentalCard.createRentalCard(member()); Item item = item(1); rentalCard.rentItem(item); rentalCard.getRentItemList().getFirst().setOverdueDate(LocalDate.now().minusDays(2)); rentalCard.returnItem(item, LocalDate.now()); rentalCard.makeAvailableRental(20); assertThat(rentalCard.getLateFee().getPoint()).isZero(); assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_AVAILABLE); } 대여 보상 동작이 대여 목록에서 도서를 제거하는지 검증합니다. @Test void cancleRentItem() { RentalCard rentalCard = RentalCard.createRentalCard(member()); Item item = item(1); rentalCard.rentItem(item); rentalCard.cancleRentItem(item); assertThat(rentalCard.getRentItemList()).isEmpty(); } 반납 보상 동작이 반납 목록을 되돌리고 대여 목록을 복원하는지 검증합니다. @Test void cancleReturnItem() { RentalCard rentalCard = RentalCard.createRentalCard(member()); Item item = item(1); rentalCard.rentItem(item); rentalCard.returnItem(item, LocalDate.now()); rentalCard.cancleReturnItem(item, 10); assertThat(rentalCard.getReturnItemList()).isEmpty(); assertThat(rentalCard.getRentItemList()).hasSize(1); } 연체 해제 보상 동작이 연체료와 대여 정지 상태를 복구하는지 검증합니다. @Test void cancleMakeAvailableRental() { RentalCard rentalCard = RentalCard.createRentalCard(member()); rentalCard.cancleMakeAvailableRental(40); assertThat(rentalCard.getLateFee().getPoint()).isEqualTo(40); assertThat(rentalCard.getRentStatus()).isEqualTo(RentStatus.RENT_UNAVAILABLE); } 테스트용 회원 식별 값을 생성합니다.
     *
     * @return 대여카드 테스트에 사용할 회원 ID와 이름을 반환합니다.
     */
    private IDName member() {
        return new IDName("jenny", "제니");
    }

    /**
     * 테스트용 도서 값을 생성합니다.
     *
     * @param no 도서 번호입니다.
     * @return 대여카드 테스트에 사용할 도서 번호와 제목을 반환합니다.
     */
    private Item item(long no) {
        return new Item(no, "book-" + no);
    }
}
