package com.example.library.member.adapter.in.web.request;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.dto.ChangePointCommand;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 회원 포인트 변경 요청 DTO.
 *
 * @param point 적립, 차감, 정산 또는 보상에 사용할 포인트 값입니다.
 */
public record PointRequest(
        @PositiveOrZero long point
) {
    /**
     * 회원 포인트 변경 요청 DTO -> command 로 변환합니다.
      * @param id 회원 ID
     * @return 회원 포인트 적립 command
     */
    public ChangePointCommand toCommand(String id) {
        return new ChangePointCommand(new IDName(id, null), point);
    }
}
