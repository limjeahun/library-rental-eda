package com.example.library.member.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

public interface UsePointUsecase {
    Member userPoint(IDName idName, long point);

    default Member usePoint(IDName idName, long point) {
        return userPoint(idName, point);
    }
}
