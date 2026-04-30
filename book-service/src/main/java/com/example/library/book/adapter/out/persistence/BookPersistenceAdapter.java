package com.example.library.book.adapter.out.persistence;

import com.example.library.book.adapter.out.persistence.mapper.BookPersistenceMapper;
import com.example.library.book.adapter.out.persistence.repository.BookJpaRepository;
import com.example.library.book.application.port.out.BookOutputPort;
import com.example.library.book.domain.model.Book;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Repository;

@Repository
public class BookPersistenceAdapter implements BookOutputPort {
    private final BookJpaRepository repository;
    private final BookPersistenceMapper mapper;

    public BookPersistenceAdapter(BookJpaRepository repository, BookPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Book save(Book book) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(book)));
    }

    @Override
    public Book loadBook(long bookNo) {
        return repository.findById(bookNo)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("도서를 찾을 수 없습니다: " + bookNo));
    }
}
