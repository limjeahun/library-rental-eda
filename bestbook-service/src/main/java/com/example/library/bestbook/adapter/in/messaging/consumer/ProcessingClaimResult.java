package com.example.library.bestbook.adapter.in.messaging.consumer;

/**
 * Redis processing lock 점유 시도 결과.
 */
public enum ProcessingClaimResult {
    /**
     * 현재 Consumer 가 메시지 처리 권한을 획득한 상태.
     */
    CLAIMED,

    /**
     * 다른 Consumer 또는 스레드가 같은 메시지를 이미 처리 중인 상태.
     */
    ALREADY_PROCESSING
}
