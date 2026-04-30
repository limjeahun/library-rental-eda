package com.example.library.member.application.dto;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.PassWord;

public record AddMemberCommand(IDName idName, PassWord passWord, Email email) {
}
