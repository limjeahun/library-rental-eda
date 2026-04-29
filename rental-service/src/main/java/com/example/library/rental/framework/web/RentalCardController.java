package com.example.library.rental.framework.web;

import com.example.library.rental.application.usecase.ClearOverdueItemUsecase;
import com.example.library.rental.application.usecase.CreateRentalCardUsecase;
import com.example.library.rental.application.usecase.InquiryUsecase;
import com.example.library.rental.application.usecase.OverdueItemUsercase;
import com.example.library.rental.application.usecase.RentItemUsecase;
import com.example.library.rental.application.usecase.ReturnItemUsercase;
import com.example.library.rental.framework.web.dto.ClearOverdueInfoDTO;
import com.example.library.rental.framework.web.dto.RentItemOutputDTO;
import com.example.library.rental.framework.web.dto.RentalCardOutputDTO;
import com.example.library.rental.framework.web.dto.RentalResultOuputDTO;
import com.example.library.rental.framework.web.dto.RetrunItemOupputDTO;
import com.example.library.rental.framework.web.dto.UserInputDTO;
import com.example.library.rental.framework.web.dto.UserItemInputDTO;
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
    private final CreateRentalCardUsecase createRentalCardUsecase;
    private final RentItemUsecase rentItemUsecase;
    private final ReturnItemUsercase returnItemUsercase;
    private final OverdueItemUsercase overdueItemUsercase;
    private final ClearOverdueItemUsecase clearOverdueItemUsecase;
    private final InquiryUsecase inquiryUsecase;

    public RentalCardController(
        CreateRentalCardUsecase createRentalCardUsecase,
        RentItemUsecase rentItemUsecase,
        ReturnItemUsercase returnItemUsercase,
        OverdueItemUsercase overdueItemUsercase,
        ClearOverdueItemUsecase clearOverdueItemUsecase,
        InquiryUsecase inquiryUsecase
    ) {
        this.createRentalCardUsecase = createRentalCardUsecase;
        this.rentItemUsecase = rentItemUsecase;
        this.returnItemUsercase = returnItemUsercase;
        this.overdueItemUsercase = overdueItemUsercase;
        this.clearOverdueItemUsecase = clearOverdueItemUsecase;
        this.inquiryUsecase = inquiryUsecase;
    }

    @PostMapping("/")
    public RentalCardOutputDTO createRentalCard(@Valid @RequestBody UserInputDTO userInputDTO) {
        return RentalCardOutputDTO.from(createRentalCardUsecase.createRentalCard(userInputDTO.toIdName()));
    }

    @GetMapping("/{id}")
    public RentalCardOutputDTO getRentalCard(@PathVariable String id) {
        return RentalCardOutputDTO.from(inquiryUsecase.getRentalCard(id));
    }

    @GetMapping("/{id}/rentbook")
    public List<RentItemOutputDTO> getRentBooks(@PathVariable String id) {
        return inquiryUsecase.getRentItems(id).stream().map(RentItemOutputDTO::from).toList();
    }

    @GetMapping("/{id}/returnbook")
    public List<RetrunItemOupputDTO> getReturnBooks(@PathVariable String id) {
        return inquiryUsecase.getReturnItems(id).stream().map(RetrunItemOupputDTO::from).toList();
    }

    @PostMapping("/rent")
    public RentalResultOuputDTO rent(@Valid @RequestBody UserItemInputDTO userItemInputDTO) {
        return RentalResultOuputDTO.of(
            "도서 대여 이벤트를 발행했습니다.",
            RentalCardOutputDTO.from(rentItemUsecase.rentItem(userItemInputDTO.toIdName(), userItemInputDTO.toItem()))
        );
    }

    @PostMapping("/return")
    public RentalResultOuputDTO returnItem(@Valid @RequestBody UserItemInputDTO userItemInputDTO) {
        return RentalResultOuputDTO.of(
            "도서 반납 이벤트를 발행했습니다.",
            RentalCardOutputDTO.from(returnItemUsercase.returnItem(userItemInputDTO.toIdName(), userItemInputDTO.toItem(), LocalDate.now()))
        );
    }

    @PostMapping("/overdue")
    public RentalCardOutputDTO overdue(@Valid @RequestBody UserItemInputDTO userItemInputDTO) {
        return RentalCardOutputDTO.from(overdueItemUsercase.overdueItem(userItemInputDTO.toIdName(), userItemInputDTO.toItem()));
    }

    @PostMapping("/clearoverdue")
    public RentalResultOuputDTO clearOverdue(@Valid @RequestBody ClearOverdueInfoDTO clearOverdueInfoDTO) {
        return RentalResultOuputDTO.of(
            "연체해제 이벤트를 발행했습니다.",
            RentalCardOutputDTO.from(clearOverdueItemUsecase.clearOverdue(clearOverdueInfoDTO.toIdName(), clearOverdueInfoDTO.getPoint()))
        );
    }
}
