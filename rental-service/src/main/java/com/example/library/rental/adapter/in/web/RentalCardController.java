package com.example.library.rental.adapter.in.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/RentalCard")
public class RentalCardController {
    private final CreateRentalCardUseCase createRentalCardUseCase;
    private final RentItemUseCase rentItemUseCase;
    private final ReturnItemUseCase returnItemUseCase;
    private final OverdueItemUseCase overdueItemUseCase;
    private final ClearOverdueItemUseCase clearOverdueItemUseCase;
    private final RentalCardQueryUseCase rentalCardQueryUseCase;

    public RentalCardController(
        CreateRentalCardUseCase createRentalCardUseCase,
        RentItemUseCase rentItemUseCase,
        ReturnItemUseCase returnItemUseCase,
        OverdueItemUseCase overdueItemUseCase,
        ClearOverdueItemUseCase clearOverdueItemUseCase,
        RentalCardQueryUseCase rentalCardQueryUseCase
    ) {
        this.createRentalCardUseCase = createRentalCardUseCase;
        this.rentItemUseCase = rentItemUseCase;
        this.returnItemUseCase = returnItemUseCase;
        this.overdueItemUseCase = overdueItemUseCase;
        this.clearOverdueItemUseCase = clearOverdueItemUseCase;
        this.rentalCardQueryUseCase = rentalCardQueryUseCase;
    }

    @PostMapping("/")
    public RentalCardResponse createRentalCard(@Valid @RequestBody UserRequest request) {
        return RentalCardResponse.from(createRentalCardUseCase.createRentalCard(request.toIdName()));
    }

    @GetMapping("/{id}")
    public RentalCardResponse getRentalCard(@PathVariable String id) {
        return RentalCardResponse.from(rentalCardQueryUseCase.getRentalCard(id));
    }

    @GetMapping("/{id}/rentbook")
    public List<RentItemResponse> getRentBooks(@PathVariable String id) {
        return rentalCardQueryUseCase.getRentItems(id).stream().map(RentItemResponse::from).toList();
    }

    @GetMapping("/{id}/returnbook")
    public List<ReturnItemResponse> getReturnBooks(@PathVariable String id) {
        return rentalCardQueryUseCase.getReturnItems(id).stream().map(ReturnItemResponse::from).toList();
    }

    @PostMapping("/rent")
    public RentalResultResponse rent(@Valid @RequestBody UserItemRequest request) {
        return RentalResultResponse.of(
            "도서 대여 이벤트를 발행했습니다.",
            RentalCardResponse.from(rentItemUseCase.rentItem(request.toIdName(), request.toItem()))
        );
    }

    @PostMapping("/return")
    public RentalResultResponse returnItem(@Valid @RequestBody UserItemRequest request) {
        return RentalResultResponse.of(
            "도서 반납 이벤트를 발행했습니다.",
            RentalCardResponse.from(returnItemUseCase.returnItem(request.toIdName(), request.toItem(), LocalDate.now()))
        );
    }

    @PostMapping("/overdue")
    public RentalCardResponse overdue(@Valid @RequestBody UserItemRequest request) {
        return RentalCardResponse.from(overdueItemUseCase.overdueItem(request.toIdName(), request.toItem()));
    }

    @PostMapping("/clearoverdue")
    public RentalResultResponse clearOverdue(@Valid @RequestBody ClearOverdueRequest request) {
        return RentalResultResponse.of(
            "연체해제 이벤트를 발행했습니다.",
            RentalCardResponse.from(clearOverdueItemUseCase.clearOverdue(request.toIdName(), request.point()))
        );
    }
}
