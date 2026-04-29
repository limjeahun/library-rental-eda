package com.example.library.member.application.usecase;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Member;

public interface SavePointUsecase {
    Member savePoint(IDName idName, long point);
}
