package com.example.library.bestbook.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.bestbook.adapter.out.persistence.document.BestBookDocument;
import com.example.library.bestbook.domain.model.BestBook;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BestBookPersistenceMapperTest {
    private final BestBookPersistenceMapper mapper = Mappers.getMapper(BestBookPersistenceMapper.class);

    @Test
    void mapsNewBestBookToDocumentUsingItemNoAsDocumentId() {
        BestBook bestBook = new BestBook(null, 1L, "도서1", 3L);

        BestBookDocument document = mapper.toDocument(bestBook);

        assertThat(document.getId()).isEqualTo(bestBook.getItemNo());
        assertThat(document.getItemNo()).isEqualTo(bestBook.getItemNo());
        assertThat(document.getItemTitle()).isEqualTo(bestBook.getItemTitle());
        assertThat(document.getRentCount()).isEqualTo(bestBook.getRentCount());
    }

    @Test
    void mapsPersistedBestBookToDocumentUsingExistingDocumentId() {
        BestBook bestBook = new BestBook(10L, 1L, "도서1", 3L);

        BestBookDocument document = mapper.toDocument(bestBook);

        assertThat(document.getId()).isEqualTo(bestBook.getId());
        assertThat(document.getItemNo()).isEqualTo(bestBook.getItemNo());
        assertThat(document.getItemTitle()).isEqualTo(bestBook.getItemTitle());
        assertThat(document.getRentCount()).isEqualTo(bestBook.getRentCount());
    }

    @Test
    void mapsDocumentToBestBook() {
        BestBookDocument document = new BestBookDocument(10L, 1L, "도서1", 3L);

        BestBook bestBook = mapper.toDomain(document);

        assertThat(bestBook.getId()).isEqualTo(document.getId());
        assertThat(bestBook.getItemNo()).isEqualTo(document.getItemNo());
        assertThat(bestBook.getItemTitle()).isEqualTo(document.getItemTitle());
        assertThat(bestBook.getRentCount()).isEqualTo(document.getRentCount());
    }
}
