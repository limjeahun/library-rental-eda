package com.example.library.member.application.dto;

import com.example.library.common.vo.IDName;

public record ChangePointCommand(IDName idName, long point) {
}
