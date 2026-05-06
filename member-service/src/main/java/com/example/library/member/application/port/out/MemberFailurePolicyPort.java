package com.example.library.member.application.port.out;

/**
 * 테스트/운영 설정에 따른 회원 참여 흐름 실패 주입 여부를 제공하는 outbound port입니다.
 */
public interface MemberFailurePolicyPort {
    boolean shouldFailOverdueClear();
}
