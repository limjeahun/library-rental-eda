package com.example.library.bestbook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.common.vo.Item;
import org.junit.jupiter.api.Test;

class BestBookTest {
    @Test
    void registerBestBook() {
        BestBook bestBook = BestBook.registerBestBook(new Item(1L, "도서"));

        assertThat(bestBook.getItemNo()).isEqualTo(1L);
        assertThat(bestBook.getRentCount()).isEqualTo(1L);
    }

    @Test
    void increaseBestBookCount() {
        BestBook bestBook = BestBook.registerBestBook(new Item(1L, "도서"));

        bestBook.increaseBestBookCount();

        assertThat(bestBook.getRentCount()).isEqualTo(2L);
    }
}
