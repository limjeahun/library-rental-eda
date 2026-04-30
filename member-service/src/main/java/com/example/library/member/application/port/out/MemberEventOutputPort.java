package com.example.library.member.application.port.out;

import com.example.library.common.event.EventResult;

public interface MemberEventOutputPort {
    void publish(EventResult result);
}
