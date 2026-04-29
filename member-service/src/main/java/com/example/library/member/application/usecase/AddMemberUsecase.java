package com.example.library.member.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.PassWord;

public interface AddMemberUsecase {
    Member addMember(IDName idName, PassWord passWord, Email email);
}
