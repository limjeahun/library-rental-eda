package com.example.library.book.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.library.book.application.dto.BookResult;
import com.example.library.book.application.port.out.LoadBookPort;
import com.example.library.book.application.port.out.SaveBookPort;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.Classification;
import com.example.library.book.domain.model.Location;
import com.example.library.book.domain.model.Source;
import com.example.library.book.domain.vo.BookDesc;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock
    private LoadBookPort loadBookPort;

    @Mock
    private SaveBookPort saveBookPort;

    @InjectMocks
    private BookService service;

    @Test
    void getBooksReturnsAllLoadedBooks() {
        given(loadBookPort.loadBooks()).willReturn(List.of(
            book(1L, "도서1", BookStatus.AVAILABLE),
            book(2L, "도서2", BookStatus.UNAVAILABLE)
        ));

        List<BookResult> results = service.getBooks();

        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(BookResult::no)
            .containsExactly(1L, 2L);
        assertThat(results)
            .extracting(BookResult::title)
            .containsExactly("도서1", "도서2");
        verify(loadBookPort).loadBooks();
    }

    private Book book(Long no, String title, BookStatus status) {
        return Book.reconstitute(
            no,
            title,
            new BookDesc("설명", "저자", "isbn", LocalDate.of(2026, 5, 16), Source.SUPPLY),
            Classification.LITERATURE,
            status,
            Location.JEONGJA
        );
    }
}
