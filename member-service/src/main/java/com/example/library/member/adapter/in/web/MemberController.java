package com.example.library.member.adapter.in.web;

import com.example.library.common.core.web.BaseResponse;
import com.example.library.member.adapter.in.web.request.MemberRequest;
import com.example.library.member.adapter.in.web.response.MemberResponse;
import com.example.library.member.adapter.in.web.request.PointRequest;
import com.example.library.member.application.port.in.AddMemberUseCase;
import com.example.library.member.application.port.in.MemberQueryUseCase;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 등록/조회와 포인트 적립/사용 HTTP 요청을 처리하는 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/Member")
@RequiredArgsConstructor
public class MemberController {
    private final AddMemberUseCase      addMemberUseCase;
    private final MemberQueryUseCase    memberQueryUseCase;
    private final SavePointUseCase      savePointUseCase;
    private final UsePointUseCase       usePointUseCase;

    /**
     * 새 회원 등록.
     *
     * @param request 회원 ID, 이름, 이메일, 비밀번호를 담은 등록 요청 본문 DTO.
     * @return 클회원 정보 DTO.
     */
    @PostMapping("/")
    public ResponseEntity<BaseResponse<MemberResponse>> addMember(@Valid @RequestBody MemberRequest request) {
        return BaseResponse.created(
                MemberResponse.from(
                        addMemberUseCase.addMember(request.toCommand())
                )
        ).toResponseEntity();
    }

    /**
     * 회원 번호로 단건 회원을 조회.
     *
     * @param no 조회할 회원 번호.
     * @return 회원 정보 DTO.
     */
    @GetMapping("/{no}")
    public ResponseEntity<BaseResponse<MemberResponse>> getMember(@PathVariable long no) {
        return BaseResponse.ok(
                MemberResponse.from(memberQueryUseCase.getMember(no))
        ).toResponseEntity();
    }

    /**
     * 회원 로그인 ID로 단건 회원을 조회.
     *
     * @param id 조회하거나 포인트를 변경할 회원 ID.
     * @return 회원 정보 DTO.
     */
    @GetMapping("/by-id/{id}")
    public ResponseEntity<BaseResponse<MemberResponse>> getMemberById(@PathVariable String id) {
        return BaseResponse.ok(
                MemberResponse.from(memberQueryUseCase.getMemberById(id))
        ).toResponseEntity();
    }

    /**
     * 지정 회원에게 포인트를 적립.
     *
     * @param id 조회하거나 포인트를 변경할 회원 ID.
     * @param request 적립하거나 사용할 포인트 값을 담은 요청 본문 DTO.
     * @return 회원 정보 DTO.
     */
    @PostMapping("/{id}/points/save")
    public ResponseEntity<BaseResponse<MemberResponse>> savePoint(@PathVariable String id, @Valid @RequestBody PointRequest request) {
        return BaseResponse.ok(
                MemberResponse.from(savePointUseCase.savePoint(request.toCommand(id)))
        ).toResponseEntity();
    }

    /**
     * 지정 회원의 포인트를 사용.
     *
     * @param id 조회하거나 포인트를 변경할 회원 ID.
     * @param request 적립하거나 사용할 포인트 값을 담은 요청 본문 DTO.
     * @return 회원 정보 DTO.
     */
    @PostMapping("/{id}/points/use")
    public ResponseEntity<BaseResponse<MemberResponse>> usePoint(@PathVariable String id, @Valid @RequestBody PointRequest request) {
        return BaseResponse.ok(
            MemberResponse.from(usePointUseCase.usePoint(request.toCommand(id)))
        ).toResponseEntity();
    }
}
