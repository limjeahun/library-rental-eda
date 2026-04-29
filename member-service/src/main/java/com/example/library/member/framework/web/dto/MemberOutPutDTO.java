package com.example.library.member.framework.web.dto;

import com.example.library.member.domain.model.Member;
import com.example.library.member.domain.model.UserRole;
import java.util.List;

public class MemberOutPutDTO {
    private Long memberNo;
    private String id;
    private String name;
    private String email;
    private List<UserRole> authorites;
    private long point;

    public MemberOutPutDTO() {
    }

    public MemberOutPutDTO(Long memberNo, String id, String name, String email, List<UserRole> authorites, long point) {
        this.memberNo = memberNo;
        this.id = id;
        this.name = name;
        this.email = email;
        this.authorites = authorites;
        this.point = point;
    }

    public static MemberOutPutDTO from(Member member) {
        return new MemberOutPutDTO(
            member.getMemberNo(),
            member.getIdName().getId(),
            member.getIdName().getName(),
            member.getEmail().getValue(),
            member.getAuthorites().stream().map(authority -> authority.getRole()).toList(),
            member.getPoint().getPoint()
        );
    }

    public Long getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(Long memberNo) {
        this.memberNo = memberNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UserRole> getAuthorites() {
        return authorites;
    }

    public void setAuthorites(List<UserRole> authorites) {
        this.authorites = authorites;
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}
