package com.example.library.rental.application.port.in;

import com.example.library.common.vo.IDName;
import com.example.library.common.vo.Item;
import com.example.library.rental.domain.model.RentalCard;

/**
 * 대여 중인 도서를 연체로 표시하고 대여카드를 대여 정지 상태로 바꾸는 application 계약입니다.
 */
public interface OverdueItemUseCase {
    /**
     * 지정 회원의 대여 도서를 연체 처리합니다.
     *
     * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 대상 도서가 연체 표시되고 대여 정지 상태로 저장된 대여카드를 반환합니다.
     */
    RentalCard overdueItem(IDName idName, Item item);
}
