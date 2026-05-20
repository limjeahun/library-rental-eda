package com.example.library.member.application.dto;

/**
 * 회원 등록 업무에 필요한 입력 값을 담은 application command입니다.
 *
 * @param id 대상 회원 ID.
 * @param name 대상 회원 이름.
 * @param passWord 비밀번호 값.
 * @param email 이메일 값.
 */
public record AddMemberCommand(String id, String name, String passWord, String email) {
}
