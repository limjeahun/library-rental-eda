package com.example.library.rental.adapter.in.web;

import com.example.library.common.core.web.BaseResponse;
import com.example.library.rental.adapter.in.web.dto.ClearOverdueRequest;
import com.example.library.rental.adapter.in.web.dto.RentItemResponse;
import com.example.library.rental.adapter.in.web.dto.RentalCardResponse;
import com.example.library.rental.adapter.in.web.dto.RentalResultResponse;
import com.example.library.rental.adapter.in.web.dto.ReturnItemResponse;
import com.example.library.rental.adapter.in.web.dto.UserItemRequest;
import com.example.library.rental.adapter.in.web.dto.UserRequest;
import com.example.library.rental.application.port.in.ClearOverdueItemUseCase;
import com.example.library.rental.application.port.in.CreateRentalCardUseCase;
import com.example.library.rental.application.port.in.OverdueItemUseCase;
import com.example.library.rental.application.port.in.RentItemUseCase;
import com.example.library.rental.application.port.in.RentalCardQueryUseCase;
import com.example.library.rental.application.port.in.ReturnItemUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대여카드 생성, 도서 대여/반납, 연체 표시, 연체료 정산, 대여카드 조회 요청을 처리.
 */
@RestController
@RequestMapping("/api/rental-cards")
@RequiredArgsConstructor
public class RentalCardController {
    private final CreateRentalCardUseCase   createRentalCardUseCase;
    private final RentItemUseCase           rentItemUseCase;
    private final ReturnItemUseCase         returnItemUseCase;
    private final OverdueItemUseCase        overdueItemUseCase;
    private final ClearOverdueItemUseCase   clearOverdueItemUseCase;
    private final RentalCardQueryUseCase    rentalCardQueryUseCase;

    /**
     * 회원 요청으로 대여카드를 생성하거나 기존 카드를 반환.
     *
     * @param request 대여카드를 만들 회원 ID와 이름을 담은 요청 본문 DTO.
     * @return 클라이언트에 반환할 HTTP 응답 DTO 를 반환.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<RentalCardResponse>> createRentalCard(@Valid @RequestBody UserRequest request) {
        return BaseResponse.ok(
                    RentalCardResponse.from(
                        createRentalCardUseCase.createRentalCard(request.toIdName())
                    )
                )
            .toResponseEntity();
    }

    /**
     * 회원 ID로 대여카드를 조회.
     *
     * @param memberId 조회 대상 회원 ID.
     * @return 클라이언트에 반환할 HTTP 응답 DTO 를 반환.
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<BaseResponse<RentalCardResponse>> getRentalCard(@PathVariable String memberId) {
        return BaseResponse.ok(
                    RentalCardResponse.from(
                            rentalCardQueryUseCase.getRentalCard(memberId)
                    )
                )
            .toResponseEntity();
    }

    /**
     * 회원 ID로 현재 대여 중인 도서 목록을 조회.
     *
     * @param memberId 조회 대상 회원 ID.
     * @return 조회 결과를 HTTP 응답 DTO 목록으로 변환해 반환.
     */
    @GetMapping("/{memberId}/rent-items")
    public ResponseEntity<BaseResponse<List<RentItemResponse>>> getRentItems(@PathVariable String memberId) {
        return BaseResponse.ok(
                    rentalCardQueryUseCase.getRentItems(memberId).stream()
                            .map(RentItemResponse::from)
                            .toList()
                )
            .toResponseEntity();
    }

    /**
     * 회원 ID로 반납 완료된 도서 목록을 조회.
     *
     * @param memberId 조회 대상 회원 ID.
     * @return 조회 결과를 HTTP 응답 DTO 목록으로 변환해 반환.
     */
    @GetMapping("/{memberId}/return-items")
    public ResponseEntity<BaseResponse<List<ReturnItemResponse>>> getReturnItems(@PathVariable String memberId) {
        return BaseResponse.ok(
                    rentalCardQueryUseCase.getReturnItems(memberId).stream()
                            .map(ReturnItemResponse::from)
                            .toList()
                )
            .toResponseEntity();
    }

    /**
     * 도서 대여를 처리하고 후속 Kafka 이벤트 발행 결과 메시지를 반환.
     *
     * @param request 대여할 회원 ID/이름과 도서 번호/제목을 담은 요청 본문 DTO.
     * @return 클라이언트에 반환할 HTTP 응답 DTO 를 반환.
     */
    @PostMapping("/rent")
    public ResponseEntity<BaseResponse<RentalResultResponse>> rent(@Valid @RequestBody UserItemRequest request) {
        return BaseResponse.accepted(
                RentalResultResponse.of(
                        "도서 대여 이벤트를 발행했습니다.",
                        RentalCardResponse.from(
                                rentItemUseCase.rentItem(request.toIdName(), request.toItem())
                        )
                )
        ).toResponseEntity();
    }

    /**
     * 도서 반납을 처리하고 후속 Kafka 이벤트 발행 결과 메시지를 반환합니다.
     *
     * @param request 반납할 도서 번호/제목과 회원 ID/이름을 담은 요청 본문 DTO입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping("/return")
    public ResponseEntity<BaseResponse<RentalResultResponse>> returnItem(@Valid @RequestBody UserItemRequest request) {
        return BaseResponse.accepted(RentalResultResponse.of(
            "도서 반납 이벤트를 발행했습니다.",
            RentalCardResponse.from(returnItemUseCase.returnItem(request.toIdName(), request.toItem(), LocalDate.now()))
        )).toResponseEntity();
    }

    /**
     * 대여 중인 도서를 연체 상태로 표시합니다.
     *
     * @param request 연체로 표시할 회원 ID/이름과 도서 번호/제목을 담은 요청 본문 DTO입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping("/overdue")
    public ResponseEntity<BaseResponse<RentalCardResponse>> overdue(@Valid @RequestBody UserItemRequest request) {
        return BaseResponse.ok(RentalCardResponse.from(overdueItemUseCase.overdueItem(request.toIdName(), request.toItem())))
            .toResponseEntity();
    }

    /**
     * 연체료를 정산하고 후속 Kafka 이벤트 발행 결과 메시지를 반환합니다.
     *
     * @param request 연체료 정산에 사용할 회원 ID/이름과 포인트를 담은 요청 본문 DTO입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping("/clear-overdue")
    public ResponseEntity<BaseResponse<RentalResultResponse>> clearOverdue(@Valid @RequestBody ClearOverdueRequest request) {
        return BaseResponse.accepted(RentalResultResponse.of(
            "연체해제 이벤트를 발행했습니다.",
            RentalCardResponse.from(clearOverdueItemUseCase.clearOverdue(request.toIdName(), request.point()))
        )).toResponseEntity();
    }
}
