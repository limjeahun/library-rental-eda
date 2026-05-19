package com.example.library.bestbook.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.bestbook.adapter.out.persistence.document.BestBookDocument;
import com.example.library.bestbook.domain.model.BestBook;
import org.junit.jupiter.api.Test;

class BestBookPersistenceMapperTest {
    private final BestBookPersistenceMapper mapper = new BestBookPersistenceMapper();

    @Test
    void mapsNewBestBookToDocumentUsingItemNoAsDocumentId() {
        BestBook bestBook = BestBook.reconstitute(null, 1L, "도서1", 3L);

        BestBookDocument document = mapper.toDocument(bestBook);

        assertThat(document.getId()).isEqualTo(bestBook.itemNo());
        assertThat(document.getItemNo()).isEqualTo(bestBook.itemNo());
        assertThat(document.getItemTitle()).isEqualTo(bestBook.itemTitle());
        assertThat(document.getRentCount()).isEqualTo(bestBook.rentCount());
    }

    @Test
    void mapsPersistedBestBookToDocumentUsingExistingDocumentId() {
        BestBook bestBook = BestBook.reconstitute(10L, 1L, "도서1", 3L);

        BestBookDocument document = mapper.toDocument(bestBook);

        assertThat(document.getId()).isEqualTo(bestBook.id());
        assertThat(document.getItemNo()).isEqualTo(bestBook.itemNo());
        assertThat(document.getItemTitle()).isEqualTo(bestBook.itemTitle());
        assertThat(document.getRentCount()).isEqualTo(bestBook.rentCount());
    }

    @Test
    void mapsDocumentToBestBook() {
        BestBookDocument document = new BestBookDocument(10L, 1L, "도서1", 3L);

        BestBook bestBook = mapper.toDomain(document);

        assertThat(bestBook.id()).isEqualTo(document.getId());
        assertThat(bestBook.itemNo()).isEqualTo(document.getItemNo());
        assertThat(bestBook.itemTitle()).isEqualTo(document.getItemTitle());
        assertThat(bestBook.rentCount()).isEqualTo(document.getRentCount());
    }
}
