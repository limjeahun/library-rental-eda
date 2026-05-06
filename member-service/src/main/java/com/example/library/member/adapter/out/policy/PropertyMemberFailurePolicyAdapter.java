package com.example.library.member.adapter.out.policy;

import com.example.library.member.application.port.out.MemberFailurePolicyPort;
import com.example.library.member.config.MemberFailureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Spring configuration properties를 application failure policy port로 변환하는 adapter입니다.
 */
@Component
@RequiredArgsConstructor
public class PropertyMemberFailurePolicyAdapter implements MemberFailurePolicyPort {
    private final MemberFailureProperties properties;

    @Override
    public boolean shouldFailOverdueClear() {
        return properties.forceOverdueClearFail();
    }
}
