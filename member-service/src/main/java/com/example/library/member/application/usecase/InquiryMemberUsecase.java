package com.example.library.member.application.usecase;

import com.example.library.member.domain.model.Member;

public interface InquiryMemberUsecase {
    Member getMember(long memberNo);

    Member getMemberById(String id);
}
