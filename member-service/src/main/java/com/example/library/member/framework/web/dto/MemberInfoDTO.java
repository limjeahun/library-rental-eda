package com.example.library.member.framework.web.dto;

import com.example.library.common.vo.IDName;
import com.example.library.member.domain.model.Email;
import com.example.library.member.domain.model.PassWord;
import jakarta.validation.constraints.NotBlank;

public class MemberInfoDTO {
    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String passWord;

    @NotBlank
    @jakarta.validation.constraints.Email
    private String email;

    public MemberInfoDTO() {
    }

    public IDName toIdName() {
        return new IDName(id, name);
    }

    public PassWord toPassWord() {
        return new PassWord(passWord);
    }

    public Email toEmail() {
        return new Email(email);
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

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
