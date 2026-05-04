package com.example.library.member.application.dto;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.vo.Email;
import com.example.library.member.domain.vo.PassWord;

/**
 * 회원 등록 업무에 필요한 회원 ID, 이름, 이메일, 비밀번호를 담은 application command입니다.
 *
 * @param idName 대상 회원의 ID와 이름을 담은 공통 값 객체입니다.
 * @param passWord 저장하거나 검증할 비밀번호 값입니다.
 * @param email 저장하거나 검증할 이메일 값입니다.
 */
public record AddMemberCommand(IDName idName, PassWord passWord, Email email) {
}
