package com.example.library.member.application.dto;

/**
 * 회원 등록 업무에 필요한 회원 ID, 이름, 이메일, 비밀번호를 담은 application command입니다.
 *
 * @param id 대상 회원 ID입니다.
 * @param name 대상 회원 이름입니다.
 * @param passWord 저장하거나 검증할 비밀번호 값입니다.
 * @param email 저장하거나 검증할 이메일 값입니다.
 */
public record AddMemberCommand(String id, String name, String passWord, String email) {
}
