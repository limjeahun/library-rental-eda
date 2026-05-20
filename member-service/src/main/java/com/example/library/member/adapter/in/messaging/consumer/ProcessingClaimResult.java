package com.example.library.member.adapter.in.messaging.consumer;

/**
 * Redis 메시지 처리 lock 점유 결과.
 */
public enum ProcessingClaimResult {
    /**
     * 현재 consumer가 처리권을 얻은 상태.
     */
    CLAIMED,

    /**
     * 같은 메시지가 이미 처리 중인 상태.
     */
    ALREADY_PROCESSING
}
