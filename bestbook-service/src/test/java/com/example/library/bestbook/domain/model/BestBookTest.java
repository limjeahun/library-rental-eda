package com.example.library.bestbook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.common.vo.Item;
import org.junit.jupiter.api.Test;

/**
 * 인기 도서 도메인 모델의 등록과 대여 횟수 증가 규칙을 검증합니다.
 */
class BestBookTest {
    /**
     * 첫 대여 기록이 인기 도서 모델을 생성하고 대여 횟수를 1로 설정하는지 검증합니다.
     */
    @Test
    void registerBestBook() {
        BestBook bestBook = BestBook.registerBestBook(new Item(1L, "도서"));

        assertThat(bestBook.getItemNo()).isEqualTo(1L);
        assertThat(bestBook.getRentCount()).isEqualTo(1L);
    }

    /**
     * 기존 인기 도서의 대여 횟수가 증가하는지 검증합니다.
     */
    @Test
    void increaseBestBookCount() {
        BestBook bestBook = BestBook.registerBestBook(new Item(1L, "도서"));

        bestBook.increaseBestBookCount();

        assertThat(bestBook.getRentCount()).isEqualTo(2L);
    }
}
