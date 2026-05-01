package com.example.library.member.adapter.in.web.request;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.dto.AddMemberCommand;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.PassWord;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원 등록 HTTP 요청을 표현하는 HTTP DTO입니다.
 *
 * @param id 조회하거나 포인트를 변경할 회원 ID입니다.
 * @param name 회원 이름입니다.
 * @param passWord 저장하거나 검증할 비밀번호 값입니다.
 * @param email 저장하거나 검증할 이메일 값입니다.
 */
public record MemberRequest(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String passWord,
    @NotBlank @jakarta.validation.constraints.Email String email
) {
    /**
     * web 요청 DTO를 회원 등록 application command로 변환합니다.
     *
     * @return 요청 값을 업무 처리에 사용할 application command로 변환해 반환합니다.
     */
    public AddMemberCommand toCommand() {
        return new AddMemberCommand(new IDName(id, name), new PassWord(passWord), new Email(email));
    }
}
