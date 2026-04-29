package com.example.library.book.framework.jpaadapter;

import com.example.library.book.application.outputport.BookOutPutPort;
import com.example.library.book.domain.model.Book;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Repository;

@Repository
public class BookJpaAdapter implements BookOutPutPort {
    private final BookRepository bookRepository;
    private final BookQueryRepository bookQueryRepository;

    public BookJpaAdapter(BookRepository bookRepository, BookQueryRepository bookQueryRepository) {
        this.bookRepository = bookRepository;
        this.bookQueryRepository = bookQueryRepository;
    }

    @Override
    public Book loadBook(long bookNo) {
        return bookQueryRepository.findByNo(bookNo)
            .orElseThrow(() -> new NoSuchElementException("도서를 찾을 수 없습니다."));
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }
}
