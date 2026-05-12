package com.example.library.member.adapter.in.web.request;

import com.example.library.member.application.dto.AddMemberCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원 등록 HTTP 요청 DTO.
 *
 * @param id 조회하거나 포인트를 변경할 회원 ID.
 * @param name 회원 이름.
 * @param passWord 저장하거나 검증할 비밀번호 값.
 * @param email 저장하거나 검증할 이메일 값.
 */
public record MemberRequest(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String passWord,
    @NotBlank @jakarta.validation.constraints.Email String email
) {
    /**
     * web 요청 DTO 를 회원 등록 application command 로 변환.
     *
     * @return 요청 값을 업무 처리에 사용할 application command 로 변환해 반환.
     */
    public AddMemberCommand toCommand() {
        return new AddMemberCommand(id, name, passWord, email);
    }
}
