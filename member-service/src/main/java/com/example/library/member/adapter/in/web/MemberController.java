package com.example.library.member.adapter.in.web;

import com.example.library.common.vo.IDName;
import com.example.library.member.adapter.in.web.dto.MemberRequest;
import com.example.library.member.adapter.in.web.dto.MemberResponse;
import com.example.library.member.adapter.in.web.dto.PointRequest;
import com.example.library.member.application.dto.ChangePointCommand;
import com.example.library.member.application.port.in.AddMemberUseCase;
import com.example.library.member.application.port.in.MemberQueryUseCase;
import com.example.library.member.application.port.in.SavePointUseCase;
import com.example.library.member.application.port.in.UsePointUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/Member")
public class MemberController {
    private final AddMemberUseCase addMemberUseCase;
    private final MemberQueryUseCase memberQueryUseCase;
    private final SavePointUseCase savePointUseCase;
    private final UsePointUseCase usePointUseCase;

    public MemberController(
        AddMemberUseCase addMemberUseCase,
        MemberQueryUseCase memberQueryUseCase,
        SavePointUseCase savePointUseCase,
        UsePointUseCase usePointUseCase
    ) {
        this.addMemberUseCase = addMemberUseCase;
        this.memberQueryUseCase = memberQueryUseCase;
        this.savePointUseCase = savePointUseCase;
        this.usePointUseCase = usePointUseCase;
    }

    @PostMapping("/")
    public MemberResponse addMember(@Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(addMemberUseCase.addMember(request.toCommand()));
    }

    @GetMapping("/{no}")
    public MemberResponse getMember(@PathVariable long no) {
        return MemberResponse.from(memberQueryUseCase.getMember(no));
    }

    @GetMapping("/by-id/{id}")
    public MemberResponse getMemberById(@PathVariable String id) {
        return MemberResponse.from(memberQueryUseCase.getMemberById(id));
    }

    @PostMapping("/{id}/points/save")
    public MemberResponse savePoint(@PathVariable String id, @Valid @RequestBody PointRequest request) {
        return MemberResponse.from(savePointUseCase.savePoint(new ChangePointCommand(new IDName(id, null), request.point())));
    }

    @PostMapping("/{id}/points/use")
    public MemberResponse usePoint(@PathVariable String id, @Valid @RequestBody PointRequest request) {
        return MemberResponse.from(usePointUseCase.usePoint(new ChangePointCommand(new IDName(id, null), request.point())));
    }
}
