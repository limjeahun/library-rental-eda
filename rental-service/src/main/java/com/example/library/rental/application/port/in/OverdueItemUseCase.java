package com.example.library.rental.application.port.in;

import com.example.library.rental.application.dto.OverdueItemCommand;
import com.example.library.rental.application.dto.RentalCardResult;

/**
 * 대여 중인 도서를 연체로 표시하고 대여카드를 대여 정지 상태로 변경.
 */
public interface OverdueItemUseCase {
    /**
     * 지정 회원의 대여 도서를 연체 처리.
     *
     * @param command 대상 회원과 도서 정보를 담은 입력 command.
     * @return 대상 도서가 연체 표시되고 대여 정지 상태로 저장된 대여카드를 반환.
     */
    RentalCardResult overdueItem(OverdueItemCommand command);
}
