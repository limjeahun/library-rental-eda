package com.example.library.member.framework.web;

import com.example.library.common.vo.IDName;
import com.example.library.member.application.usecase.AddMemberUsecase;
import com.example.library.member.application.usecase.InquiryMemberUsecase;
import com.example.library.member.application.usecase.SavePointUsecase;
import com.example.library.member.application.usecase.UsePointUsecase;
import com.example.library.member.framework.web.dto.MemberInfoDTO;
import com.example.library.member.framework.web.dto.MemberOutPutDTO;
import com.example.library.member.framework.web.dto.PointRequestDTO;
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
    private final AddMemberUsecase addMemberUsecase;
    private final InquiryMemberUsecase inquiryMemberUsecase;
    private final SavePointUsecase savePointUsecase;
    private final UsePointUsecase usePointUsecase;

    public MemberController(
        AddMemberUsecase addMemberUsecase,
        InquiryMemberUsecase inquiryMemberUsecase,
        SavePointUsecase savePointUsecase,
        UsePointUsecase usePointUsecase
    ) {
        this.addMemberUsecase = addMemberUsecase;
        this.inquiryMemberUsecase = inquiryMemberUsecase;
        this.savePointUsecase = savePointUsecase;
        this.usePointUsecase = usePointUsecase;
    }

    @PostMapping("/")
    public MemberOutPutDTO addMember(@Valid @RequestBody MemberInfoDTO memberInfoDTO) {
        return MemberOutPutDTO.from(addMemberUsecase.addMember(memberInfoDTO.toIdName(), memberInfoDTO.toPassWord(), memberInfoDTO.toEmail()));
    }

    @GetMapping("/{no}")
    public MemberOutPutDTO getMember(@PathVariable long no) {
        return MemberOutPutDTO.from(inquiryMemberUsecase.getMember(no));
    }

    @GetMapping("/by-id/{id}")
    public MemberOutPutDTO getMemberById(@PathVariable String id) {
        return MemberOutPutDTO.from(inquiryMemberUsecase.getMemberById(id));
    }

    @PostMapping("/{id}/points/save")
    public MemberOutPutDTO savePoint(@PathVariable String id, @Valid @RequestBody PointRequestDTO pointRequestDTO) {
        return MemberOutPutDTO.from(savePointUsecase.savePoint(new IDName(id, null), pointRequestDTO.getPoint()));
    }

    @PostMapping("/{id}/points/use")
    public MemberOutPutDTO usePoint(@PathVariable String id, @Valid @RequestBody PointRequestDTO pointRequestDTO) {
        return MemberOutPutDTO.from(usePointUsecase.userPoint(new IDName(id, null), pointRequestDTO.getPoint()));
    }
}
