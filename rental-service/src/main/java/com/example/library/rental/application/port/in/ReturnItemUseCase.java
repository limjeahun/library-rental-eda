package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;
import java.time.LocalDate;

/**
 * 대여 중인 도서를 반납 목록으로 이동하고 도서/회원 서비스가 받을 반납 이벤트를 발행.
 */
public interface ReturnItemUseCase {
    /**
     * 지정 회원의 대여 도서를 반납 처리.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체.
     * @param item 업무 대상 도서의 번호와 제목.
     * @param returnDate 도서가 실제로 반납된 날짜.
     * @return 도서가 반납 목록으로 이동하고 반납 이벤트가 발행된 대여카드를 반환.
     */
    RentalCard returnItem(IDName idName, Item item, LocalDate returnDate);
}
