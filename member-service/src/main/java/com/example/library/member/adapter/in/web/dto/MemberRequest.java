package com.example.library.member.adapter.in.web.dto;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.PassWord;
import jakarta.validation.constraints.NotBlank;

public record MemberRequest(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String passWord,
    @NotBlank @jakarta.validation.constraints.Email String email
) {
    public AddMemberCommand toCommand() {
        return new AddMemberCommand(new IDName(id, name), new PassWord(passWord), new Email(email));
    }
}
