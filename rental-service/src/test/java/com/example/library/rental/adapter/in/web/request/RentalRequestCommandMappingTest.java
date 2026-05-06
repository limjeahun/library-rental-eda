package com.example.library.rental.adapter.in.web.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.rental.application.dto.ClearOverdueCommand;
import com.example.library.rental.application.dto.CreateRentalCardCommand;
import com.example.library.rental.application.dto.OverdueItemCommand;
import com.example.library.rental.application.dto.RentItemCommand;
import com.example.library.rental.application.dto.ReturnItemCommand;
import org.junit.jupiter.api.Test;

class RentalRequestCommandMappingTest {
    @Test
    void userRequestConvertsToCreateRentalCardCommand() {
        CreateRentalCardCommand command = new UserRequest("member-1", "회원1").toCommand();

        assertThat(command.userId()).isEqualTo("member-1");
        assertThat(command.userNm()).isEqualTo("회원1");
    }

    @Test
    void rentItemRequestConvertsToRentItemCommand() {
        RentItemCommand command = new RentItemRequest(1L, "도서1", "member-1", "회원1").toCommand();

        assertThat(command.userId()).isEqualTo("member-1");
        assertThat(command.userNm()).isEqualTo("회원1");
        assertThat(command.itemNo()).isEqualTo(1L);
        assertThat(command.itemTitle()).isEqualTo("도서1");
    }

    @Test
    void returnItemRequestConvertsToReturnItemCommand() {
        ReturnItemCommand command = new ReturnItemRequest(2L, "도서2", "member-2", "회원2").toCommand();

        assertThat(command.userId()).isEqualTo("member-2");
        assertThat(command.userNm()).isEqualTo("회원2");
        assertThat(command.itemNo()).isEqualTo(2L);
        assertThat(command.itemTitle()).isEqualTo("도서2");
    }

    @Test
    void overdueItemRequestConvertsToOverdueItemCommand() {
        OverdueItemCommand command = new OverdueItemRequest(3L, "도서3", "member-3", "회원3").toCommand();

        assertThat(command.userId()).isEqualTo("member-3");
        assertThat(command.userNm()).isEqualTo("회원3");
        assertThat(command.itemNo()).isEqualTo(3L);
        assertThat(command.itemTitle()).isEqualTo("도서3");
    }

    @Test
    void clearOverdueRequestConvertsToClearOverdueCommand() {
        ClearOverdueCommand command = new ClearOverdueRequest("member-4", "회원4", 100L).toCommand();

        assertThat(command.userId()).isEqualTo("member-4");
        assertThat(command.userNm()).isEqualTo("회원4");
        assertThat(command.point()).isEqualTo(100L);
    }
}
