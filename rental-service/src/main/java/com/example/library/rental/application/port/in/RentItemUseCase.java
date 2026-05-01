package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;

/**
 * 대여카드에 도서를 추가하고 도서/회원/인기 도서 서비스가 받을 대여 이벤트를 발행하는 application 계약입니다.
 */
public interface RentItemUseCase {
    /**
     * 지정 회원에게 도서를 대여 처리합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 도서가 대여 목록에 추가되고 대여 이벤트가 발행된 대여카드를 반환합니다.
     */
    RentalCard rentItem(IDName idName, Item item);
}
