package com.example.library.member.adapter.in.web.request;

import com.example.library.member.application.dto.AddMemberCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원 등록 HTTP 요청 DTO.
 *
 * @param id 회원 ID.
 * @param name 회원 이름.
 * @param passWord 비밀번호 값.
 * @param email 이메일 값.
 */
public record MemberRequest(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String passWord,
    @NotBlank @jakarta.validation.constraints.Email String email
) {
    /**
     * web 요청 값을 회원 등록 command로 넘깁니다.
     *
     * @return 요청 값을 담은 application command.
     */
    public AddMemberCommand toCommand() {
        return new AddMemberCommand(id, name, passWord, email);
    }
}
